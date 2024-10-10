package com.dluvian.voyage.data.provider

import android.util.Log
import androidx.compose.runtime.State
import com.dluvian.voyage.core.DEBOUNCE
import com.dluvian.voyage.core.DELAY_1SEC
import com.dluvian.voyage.core.EventIdHex
import com.dluvian.voyage.core.PubkeyHex
import com.dluvian.voyage.core.SHORT_DEBOUNCE
import com.dluvian.voyage.core.utils.containsNoneIgnoreCase
import com.dluvian.voyage.core.utils.firstThenDistinctDebounce
import com.dluvian.voyage.core.utils.launchIO
import com.dluvian.voyage.data.event.OldestUsedEvent
import com.dluvian.voyage.data.model.ForcedData
import com.dluvian.voyage.data.model.SingularPubkey
import com.dluvian.voyage.data.nostr.LazyNostrSubscriber
import com.dluvian.voyage.data.nostr.NostrSubscriber
import com.dluvian.voyage.data.nostr.createNevent
import com.dluvian.voyage.data.room.AppDatabase
import com.dluvian.voyage.ui.components.row.mainEvent.ThreadReplyCtx
import com.dluvian.voyage.ui.components.row.mainEvent.ThreadRootCtx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import rust.nostr.protocol.Nip19Event
import java.util.LinkedList

private const val TAG = "ThreadProvider"

class ThreadProvider(
    private val nostrSubscriber: NostrSubscriber,
    private val lazyNostrSubscriber: LazyNostrSubscriber,
    private val room: AppDatabase,
    private val collapsedIds: Flow<Set<EventIdHex>>,
    private val annotatedStringProvider: AnnotatedStringProvider,
    private val oldestUsedEvent: OldestUsedEvent,
    private val forcedVotes: Flow<Map<EventIdHex, Boolean>>,
    private val forcedFollows: Flow<Map<PubkeyHex, Boolean>>,
    private val forcedBookmarks: Flow<Map<EventIdHex, Boolean>>,
    private val muteProvider: MuteProvider,
    private val showAuthorName: State<Boolean>,
) {

    fun getLocalRoot(
        scope: CoroutineScope,
        nevent: Nip19Event,
        isInit: Boolean
    ): Flow<ThreadRootCtx?> {
        val id = nevent.eventId().toHex()
        scope.launchIO {
            if (!room.existsDao().postExists(id = id)) {
                nostrSubscriber.subPost(nevent = nevent)
                delay(DELAY_1SEC)
            }
            if (showAuthorName.value) {
                val author = nevent.author()?.toHex() ?: room.mainEventDao().getAuthor(id = id)
                if (author != null) {
                    lazyNostrSubscriber.lazySubUnknownProfiles(
                        selection = SingularPubkey(pubkey = author)
                    )
                }
            }

            if (isInit) lazyNostrSubscriber.lazySubRepliesAndVotes(parentId = id)
            else nostrSubscriber.subVotesAndReplies(parentIds = listOf(id))
        }

        val rootFlow = room.rootPostDao().getRootPostFlow(id = id)
        val replyFlow = room.legacyReplyDao().getReplyFlow(id = id)
        val commentFlow = room.commentDao().getCommentFlow(id = id)

        return combine(
            rootFlow.firstThenDistinctDebounce(SHORT_DEBOUNCE),
            replyFlow.firstThenDistinctDebounce(SHORT_DEBOUNCE),
            commentFlow.firstThenDistinctDebounce(SHORT_DEBOUNCE),
            getForcedFlow(),
        ) { post, reply, comment, forced ->
            val threadableMainEvent = post?.mapToRootPostUI(
                forcedFollows = forced.follows,
                forcedVotes = forced.votes,
                forcedBookmarks = forced.bookmarks,
                annotatedStringProvider = annotatedStringProvider
            ) ?: reply?.mapToLegacyReplyUI(
                forcedFollows = forced.follows,
                forcedVotes = forced.votes,
                forcedBookmarks = forced.bookmarks,
                annotatedStringProvider = annotatedStringProvider
            ) ?: comment?.mapToCommentUI(
                forcedFollows = forced.follows,
                forcedVotes = forced.votes,
                forcedBookmarks = forced.bookmarks,
                annotatedStringProvider = annotatedStringProvider
            )
            threadableMainEvent?.let { ThreadRootCtx(threadableMainEvent = it) }
        }.onEach {
            oldestUsedEvent.updateOldestCreatedAt(it?.mainEvent?.createdAt)
        }
    }

    fun getParentIsAvailableFlow(scope: CoroutineScope, replyId: EventIdHex): Flow<Boolean> {
        scope.launchIO {
            val parentId = room.someReplyDao().getParentId(id = replyId) ?: return@launchIO
            if (!room.existsDao().postExists(id = parentId)) {
                Log.i(TAG, "Parent $parentId is not available yet. Subscribing to it")
                nostrSubscriber.subPost(nevent = createNevent(hex = parentId))
            }
        }

        return room.existsDao().parentExistsFlow(id = replyId)
    }

    // Unfiltered count for ProgressBar purpose
    fun getTotalReplyCount(rootId: EventIdHex): Flow<Int> {
        return room.someReplyDao().getReplyCountFlow(parentId = rootId)
            .firstThenDistinctDebounce(SHORT_DEBOUNCE)
    }

    // Don't update oldestCreatedAt in replies. They are always younger than root
    fun getReplyCtxs(
        rootId: EventIdHex,
        parentIds: Set<EventIdHex>,
    ): Flow<List<ThreadReplyCtx>> {
        val allIds = parentIds + rootId
        val legacyFlow = room.legacyReplyDao().getRepliesFlow(parentIds = allIds)
            .firstThenDistinctDebounce(DEBOUNCE)
        val commentFlow = room.commentDao().getCommentsFlow(parentIds = allIds)
            .firstThenDistinctDebounce(DEBOUNCE)
        val opPubkeyFlow = room.mainEventDao().getAuthorFlow(id = rootId)
            .firstThenDistinctDebounce(DEBOUNCE)
        val mutedWords = muteProvider.getMutedWords()

        return combine(
            legacyFlow,
            commentFlow,
            getForcedFlow(),
            opPubkeyFlow,
            collapsedIds,
        ) { replies, comments, forced, opPubkey, collapsed ->
            val result = LinkedList<ThreadReplyCtx>()
            val hasMutedWords = { str: String -> !str.containsNoneIgnoreCase(strs = mutedWords) }

            for (reply in replies) {
                if (!reply.authorIsOneself && hasMutedWords(reply.content)) continue
                val parent = result.find { it.reply.id == reply.parentId }

                if (parent?.isCollapsed == true) continue
                if (parent == null && reply.parentId != rootId) continue

                val leveledReply = reply.mapToThreadReplyCtx(
                    level = parent?.level?.plus(1) ?: 0,
                    isOp = opPubkey == reply.pubkey,
                    forcedVotes = forced.votes,
                    forcedFollows = forced.follows,
                    collapsedIds = collapsed,
                    parentIds = parentIds,
                    forcedBookmarks = forced.bookmarks,
                    annotatedStringProvider = annotatedStringProvider
                )

                if (reply.parentId == rootId) {
                    result.add(leveledReply)
                    continue
                }
                result.add(result.indexOf(parent) + 1, leveledReply)
            }

            // Comments after replies because they can reference replies, not the other way around
            for (comment in comments) {
                if (!comment.authorIsOneself && hasMutedWords(comment.content)) continue
                val parent = result.find { it.reply.id == comment.parentId }

                if (parent?.isCollapsed == true) continue
                if (parent == null && comment.parentId != rootId) continue

                val leveledComment = comment.mapToThreadReplyCtx(
                    level = parent?.level?.plus(1) ?: 0,
                    isOp = opPubkey == comment.pubkey,
                    forcedVotes = forced.votes,
                    forcedFollows = forced.follows,
                    collapsedIds = collapsed,
                    parentIds = parentIds,
                    forcedBookmarks = forced.bookmarks,
                    annotatedStringProvider = annotatedStringProvider
                )

                if (comment.parentId == rootId) {
                    result.add(leveledComment)
                    continue
                }
                result.add(result.indexOf(parent) + 1, leveledComment)
            }

            result
        }.onEach {
            nostrSubscriber.subVotesAndReplies(
                parentIds = it.map { reply -> reply.reply.getRelevantId() }
            )
            if (showAuthorName.value) {
                nostrSubscriber.subProfiles(
                    pubkeys = it.filter { reply -> reply.reply.authorName.isNullOrEmpty() }
                        .map { reply -> reply.reply.pubkey }
                )
            }
        }
    }

    private fun getForcedFlow(): Flow<ForcedData> {
        return ForcedData.combineFlows(
            votes = forcedVotes,
            follows = forcedFollows,
            bookmarks = forcedBookmarks
        )
    }
}
