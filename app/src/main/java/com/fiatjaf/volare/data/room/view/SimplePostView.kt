package com.fiatjaf.volare.data.room.view

import androidx.room.DatabaseView
import com.fiatjaf.volare.core.EventIdHex
import com.fiatjaf.volare.core.PubkeyHex

@DatabaseView(
    "SELECT mainEvent.id, " +
            "mainEvent.pubkey, " +
            "rootPost.subject, " +
            "mainEvent.content, " +
            "(SELECT EXISTS(SELECT * FROM account WHERE account.pubkey = mainEvent.pubkey)) AS authorIsOneself, " +
            "(SELECT EXISTS(SELECT * FROM friend WHERE friend.friendPubkey = mainEvent.pubkey)) AS authorIsFriend, " +
            "(SELECT EXISTS(SELECT * FROM weboftrust WHERE weboftrust.webOfTrustPubkey = mainEvent.pubkey)) AS authorIsTrusted, " +
            "(SELECT EXISTS(SELECT * FROM mute WHERE mute.mutedItem = mainEvent.pubkey AND mute.tag IS 'p')) AS authorIsMuted, " +
            "(SELECT EXISTS(SELECT * FROM profileSetItem WHERE profileSetItem.pubkey = mainEvent.pubkey)) AS authorIsInList, " +
            "(SELECT EXISTS(SELECT * FROM lock WHERE lock.pubkey = mainEvent.pubkey)) AS authorIsLocked " +
            "FROM mainEvent " +
            "LEFT JOIN rootPost ON rootPost.eventId = mainEvent.id"
)
data class SimplePostView(
    val id: EventIdHex,
    val pubkey: PubkeyHex,
    val subject: String?,
    val content: String,
    val authorIsOneself: Boolean,
    val authorIsFriend: Boolean,
    val authorIsTrusted: Boolean,
    val authorIsMuted: Boolean,
    val authorIsInList: Boolean,
    val authorIsLocked: Boolean,
)
