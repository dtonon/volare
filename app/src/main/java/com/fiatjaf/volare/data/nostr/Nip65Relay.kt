package com.fiatjaf.volare.data.nostr

data class Nip65Relay(
    val url: RelayUrl,
    val isRead: Boolean = true,
    val isWrite: Boolean = true
)
