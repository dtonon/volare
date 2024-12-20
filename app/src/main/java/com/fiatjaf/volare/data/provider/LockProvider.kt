package com.fiatjaf.volare.data.provider

import com.fiatjaf.volare.core.PubkeyHex
import com.fiatjaf.volare.data.room.dao.LockDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class LockProvider(lockDao: LockDao) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val lockedPubkeys = lockDao.getLockedPubkeysFlow()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun isLocked(pubkey: PubkeyHex) = lockedPubkeys.value.contains(pubkey)

    fun getLockedPubkeys() = lockedPubkeys.value
}
