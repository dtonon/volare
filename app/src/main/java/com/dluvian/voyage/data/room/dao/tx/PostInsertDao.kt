package com.dluvian.voyage.data.room.dao.tx

import android.util.Log
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Transaction
import com.dluvian.voyage.data.event.ValidatedReplyPost
import com.dluvian.voyage.data.event.ValidatedRootPost
import com.dluvian.voyage.data.model.RelayedItem
import com.dluvian.voyage.data.room.entity.HashtagEntity
import com.dluvian.voyage.data.room.entity.PostEntity
import com.dluvian.voyage.data.room.entity.PostRelayEntity

private const val TAG = "PostInsertDao"
@Dao
interface PostInsertDao {

    @Transaction
    suspend fun insertRootPosts(relayedPosts: Collection<RelayedItem<ValidatedRootPost>>) {
        if (relayedPosts.isEmpty()) return

        val entities = relayedPosts.map { relayedItem -> PostEntity.from(relayedItem.item) }

        internalInsertPostOrIgnore(posts = entities)
        val postRelays = relayedPosts.map {
            PostRelayEntity(postId = it.item.id.toHex(), relayUrl = it.relayUrl)
        }
        internalInsertPostRelayOrIgnore(postRelays = postRelays)

        val hashtags = relayedPosts.flatMap { post ->
            post.item.topics.map { topic ->
                HashtagEntity(postId = post.item.id.toHex(), hashtag = topic)
            }
        }
        internalInsertHashtagsOrIgnore(hashtags = hashtags)
    }

    @Transaction
    suspend fun insertReplyPosts(relayedPosts: Collection<RelayedItem<ValidatedReplyPost>>) {
        if (relayedPosts.isEmpty()) return

        val entities = relayedPosts.map { relayedItem -> PostEntity.from(relayedItem.item) }

        runCatching {
            internalInsertPostOrIgnore(posts = entities)
            val postRelays = relayedPosts.map {
                PostRelayEntity(postId = it.item.id.toHex(), relayUrl = it.relayUrl)
            }
            internalInsertPostRelayOrIgnore(postRelays = postRelays)
        }.onFailure {
            Log.w(TAG, "Failed to insert posts: ${it.message}")
        }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun internalInsertPostOrIgnore(posts: Collection<PostEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun internalInsertPostRelayOrIgnore(postRelays: Collection<PostRelayEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun internalInsertHashtagsOrIgnore(hashtags: Collection<HashtagEntity>)
}