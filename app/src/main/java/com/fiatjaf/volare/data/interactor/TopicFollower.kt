package com.fiatjaf.volare.data.interactor

import android.content.Context
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.FollowTopic
import com.fiatjaf.volare.core.LIST_CHANGE_DEBOUNCE
import com.fiatjaf.volare.core.MAX_KEYS_SQL
import com.fiatjaf.volare.core.Topic
import com.fiatjaf.volare.core.TopicEvent
import com.fiatjaf.volare.core.UnfollowTopic
import com.fiatjaf.volare.core.utils.getNormalizedTopics
import com.fiatjaf.volare.core.utils.launchIO
import com.fiatjaf.volare.core.utils.showToast
import com.fiatjaf.volare.data.event.ValidatedTopicList
import com.fiatjaf.volare.data.nostr.NostrService
import com.fiatjaf.volare.data.nostr.secs
import com.fiatjaf.volare.data.provider.RelayProvider
import com.fiatjaf.volare.data.room.dao.TopicDao
import com.fiatjaf.volare.data.room.dao.upsert.TopicUpsertDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

private const val TAG = "TopicFollower"

class TopicFollower(
    private val nostrService: NostrService,
    private val relayProvider: RelayProvider,
    private val topicUpsertDao: TopicUpsertDao,
    private val topicDao: TopicDao,
    private val snackbar: SnackbarHostState,
    private val context: Context,
    private val forcedFollowStates: MutableStateFlow<Map<Topic, Boolean>>
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun handle(action: TopicEvent) {
        when (action) {
            is FollowTopic -> handleAction(
                topic = action.topic,
                isFollowed = true,
            )

            is UnfollowTopic -> handleAction(
                topic = action.topic,
                isFollowed = false,
            )
        }
    }

    private fun handleAction(topic: Topic, isFollowed: Boolean) {
        updateForcedState(topic = topic, isFollowed = isFollowed)
        handleFollowsInBackground()
    }

    private fun updateForcedState(topic: Topic, isFollowed: Boolean) {
        synchronized(forcedFollowStates) {
            val mutable = forcedFollowStates.value.toMutableMap()
            mutable[topic] = isFollowed
            forcedFollowStates.value = mutable
        }
    }

    private var job: Job? = null
    private fun handleFollowsInBackground() {
        if (job?.isActive == true) return

        job = scope.launchIO {
            delay(LIST_CHANGE_DEBOUNCE)

            val toHandle: Map<Topic, Boolean>
            synchronized(forcedFollowStates) {
                toHandle = forcedFollowStates.value.toMap()
            }
            val topicsBefore = topicDao.getMyTopics().toSet()
            val topicsAdjusted = topicsBefore.toMutableSet()
            val toAdd = toHandle.filter { (_, bool) -> bool }.map { (topic, _) -> topic }
            topicsAdjusted.addAll(toAdd)
            val toRemove = toHandle.filter { (_, bool) -> !bool }.map { (topic, _) -> topic }
            topicsAdjusted.removeAll(toRemove.toSet())

            if (topicsAdjusted == topicsBefore) return@launchIO

            if (topicsAdjusted.size > MAX_KEYS_SQL && topicsAdjusted.size > topicsBefore.size) {
                Log.w(TAG, "New topic list is too large (${topicsAdjusted.size})")
                topicsAdjusted
                    .minus(topicsBefore)
                    .forEach { updateForcedState(topic = it, isFollowed = false) }
                val msg = context.getString(
                    R.string.following_more_than_n_topics_is_not_allowed,
                    MAX_KEYS_SQL
                )
                snackbar.showToast(scope = scope, msg = msg)
                return@launchIO
            }

            nostrService.publishTopicList(
                topics = topicsAdjusted.toList(),
                relayUrls = relayProvider.getPublishRelays(addConnected = false),
            ).onSuccess { event ->
                val topicList = ValidatedTopicList(
                    myPubkey = event.author().toHex(),
                    topics = event.getNormalizedTopics().toSet(),
                    createdAt = event.createdAt().secs()
                )
                topicUpsertDao.upsertTopics(validatedTopicList = topicList)
            }
                .onFailure {
                    Log.w(TAG, "Failed to publish topic list: ${it.message}", it)
                    snackbar.showToast(
                        scope = scope,
                        msg = context.getString(R.string.failed_to_sign_topic_list)
                    )
                }
        }
    }
}
