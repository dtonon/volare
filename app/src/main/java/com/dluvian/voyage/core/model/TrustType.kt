package com.dluvian.voyage.core.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Immutable
sealed class TrustType {
    companion object {
        @Stable
        fun from(
            isOneself: Boolean,
            isFriend: Boolean,
            isWebOfTrust: Boolean,
            isMuted: Boolean,
            isInList: Boolean,
            isLocked: Boolean,
        ): TrustType {
            return if (isOneself && isLocked) LockedOneself
            else if (isOneself) Oneself
            else if (isLocked) Locked
            else if (isMuted) Muted
            else if (isFriend) FriendTrust
            else if (isInList) IsInListTrust
            else if (isWebOfTrust) WebTrust
            else NoTrust
        }
    }
}

data object LockedOneself : TrustType()
data object Oneself : TrustType()
data object FriendTrust : TrustType()
data object IsInListTrust : TrustType()
data object WebTrust : TrustType()
data object Muted : TrustType()
data object NoTrust : TrustType()
data object Locked : TrustType()
