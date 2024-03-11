package com.dluvian.voyage.data.room.dao.tx

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.dluvian.voyage.core.PubkeyHex
import com.dluvian.voyage.data.model.ValidatedNip65
import com.dluvian.voyage.data.room.entity.Nip65Entity

@Dao
interface Nip65UpsertDao {
    @Transaction
    suspend fun upsertNip65(validatedNip65: ValidatedNip65) {
        val list = Nip65Entity.from(validatedNip65 = validatedNip65)
        val pubkey = validatedNip65.pubkey.toHex()

        val newestCreatedAt = internalGetNewestCreatedAt(pubkey = pubkey) ?: 0L
        if (validatedNip65.createdAt < newestCreatedAt) return

        if (list.isEmpty()) {
            internalDeleteList(pubkey = pubkey)
            return
        }

        internalUpsert(nip65Entities = list)
        internalDeleteOutdated(newestCreatedAt = validatedNip65.createdAt)
    }

    @Query("SELECT MAX(createdAt) FROM nip65 WHERE pubkey = :pubkey")
    suspend fun internalGetNewestCreatedAt(pubkey: PubkeyHex): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun internalUpsert(nip65Entities: Collection<Nip65Entity>)

    @Query("DELETE FROM nip65 WHERE pubkey = :pubkey")
    suspend fun internalDeleteList(pubkey: PubkeyHex)

    @Query("DELETE FROM nip65 WHERE createdAt < :newestCreatedAt")
    suspend fun internalDeleteOutdated(newestCreatedAt: Long)
}
