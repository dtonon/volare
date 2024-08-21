package com.dluvian.voyage.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import com.dluvian.voyage.core.PubkeyHex
import kotlinx.coroutines.flow.Flow

// TODO: Exclude locks in other methods

@Dao
interface FriendDao {
    @Query("SELECT friendPubkey FROM friend WHERE friendPubkey NOT IN (SELECT pubkey FROM lock)")
    fun getFriendsNoLockFlow(): Flow<List<PubkeyHex>>

    @Query("SELECT friendPubkey FROM friend WHERE friendPubkey NOT IN (SELECT friendPubkey FROM weboftrust)")
    suspend fun getFriendsWithMissingContactList(): List<PubkeyHex>

    @Query("SELECT friendPubkey FROM friend WHERE friendPubkey NOT IN (SELECT pubkey FROM nip65)")
    suspend fun getFriendsWithMissingNip65(): List<PubkeyHex>

    @Query("SELECT friendPubkey FROM friend WHERE friendPubkey NOT IN (SELECT pubkey FROM profile)")
    suspend fun getFriendsWithMissingProfile(): List<PubkeyHex>

    @Query("SELECT MAX(createdAt) FROM friend")
    suspend fun getMaxCreatedAt(): Long?
}
