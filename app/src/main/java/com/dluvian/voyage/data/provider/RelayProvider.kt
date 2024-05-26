package com.dluvian.voyage.data.provider

import android.util.Log
import androidx.compose.runtime.State
import com.dluvian.nostr_kt.Nip65Relay
import com.dluvian.nostr_kt.NostrClient
import com.dluvian.nostr_kt.RelayUrl
import com.dluvian.nostr_kt.removeTrailingSlashes
import com.dluvian.voyage.core.MAX_POPULAR_RELAYS
import com.dluvian.voyage.core.MAX_RELAYS
import com.dluvian.voyage.core.MAX_RELAYS_PER_PUBKEY
import com.dluvian.voyage.core.MAX_RELAY_CONNECTIONS
import com.dluvian.voyage.core.PubkeyHex
import com.dluvian.voyage.core.model.ConnectionStatus
import com.dluvian.voyage.core.model.Disconnected
import com.dluvian.voyage.core.putOrAdd
import com.dluvian.voyage.data.room.dao.EventRelayDao
import com.dluvian.voyage.data.room.dao.Nip65Dao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import rust.nostr.protocol.Nip19Event
import rust.nostr.protocol.Nip19Profile


private const val TAG = "RelayProvider"

class RelayProvider(
    private val nip65Dao: Nip65Dao,
    private val eventRelayDao: EventRelayDao,
    private val nostrClient: NostrClient,
    private val connectionStatuses: State<Map<RelayUrl, ConnectionStatus>>
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val myNip65 =
        nip65Dao.getMyNip65Flow().stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun getReadRelays(limit: Boolean = true, includeConnected: Boolean = false): List<RelayUrl> {
        return myNip65.value
            .filter { it.nip65Relay.isRead }
            .map { it.nip65Relay.url }
            .ifEmpty { defaultRelays }
            .preferConnected(limit = if (limit) MAX_RELAYS else Int.MAX_VALUE)
            .let {
                if (includeConnected) (it + nostrClient.getAllConnectedUrls()).distinct() else it
            }
    }

    private fun getWriteRelays(limit: Boolean = true): List<RelayUrl> {
        return myNip65.value
            .filter { it.nip65Relay.isWrite }
            .map { it.nip65Relay.url }
            .ifEmpty { defaultRelays }
            .let { if (limit) it.preferConnected(MAX_RELAYS) else it }
    }

    fun getPublishRelays(): List<RelayUrl> {
        val relays = nostrClient.getAllConnectedUrls().toMutableSet()
        relays.addAll(getWriteRelays())

        return relays.toList()
    }

    suspend fun getPublishRelays(publishTo: List<PubkeyHex>): List<RelayUrl> {
        val relays = if (publishTo.isEmpty()) mutableSetOf()
        else nip65Dao.getReadRelays(pubkeys = publishTo)
            .groupBy { it.pubkey }
            .flatMap { (_, nip65s) ->
                nip65s.map { it.nip65Relay.url }.preferConnected(MAX_RELAYS_PER_PUBKEY)
            }.toMutableSet()
        relays.addAll(getPublishRelays())

        return relays.toList()
    }

    suspend fun getObserveRelays(
        pubkey: PubkeyHex,
        limit: Boolean = true,
        includeConnected: Boolean = false
    ): List<RelayUrl> {
        val relays = nip65Dao.getWriteRelays(pubkeys = listOf(pubkey))
            .map { it.nip65Relay.url }
            .let { if (limit) it.preferConnected(MAX_RELAYS) else it }
            .toMutableSet()
        relays.addAll(getReadRelays(limit = limit))
        if (includeConnected) relays.addAll(nostrClient.getAllConnectedUrls())

        return relays.toList()
    }

    suspend fun getObserveRelays(
        nprofile: Nip19Profile,
        limit: Boolean = true,
        includeConnected: Boolean = false
    ): List<RelayUrl> {
        val foreignRelays = nprofile.relays()
            .normalize(limit = if (limit) MAX_RELAYS else Int.MAX_VALUE)
        val nip65 = getObserveRelays(
            pubkey = nprofile.publicKey().toHex(),
            limit = limit,
            includeConnected = includeConnected
        )

        return (foreignRelays + nip65).distinct()
    }

    suspend fun getObserveRelays(
        nevent: Nip19Event,
        limit: Boolean = true,
        includeConnected: Boolean = false
    ): List<RelayUrl> {
        val foreignRelays = nevent.relays()
            .normalize(limit = if (limit) MAX_RELAYS else Int.MAX_VALUE)
        val pubkey = nevent.author()?.toHex()
        val nip65 = if (pubkey != null) getObserveRelays(
            pubkey = pubkey,
            limit = limit,
            includeConnected = includeConnected
        ) else getReadRelays(includeConnected = includeConnected)

        return (foreignRelays + nip65).distinct()
    }

    suspend fun getObserveRelays(pubkeys: Collection<PubkeyHex>): Map<RelayUrl, Set<PubkeyHex>> {
        if (pubkeys.isEmpty()) return emptyMap()

        if (pubkeys.size == 1) {
            val pubkey = pubkeys.first()
            return getObserveRelays(pubkey = pubkey).associateWith { setOf(pubkey) }
        }

        val result = mutableMapOf<RelayUrl, MutableSet<PubkeyHex>>()
        val connectedRelays = nostrClient.getAllConnectedUrls().toSet()
        val eventRelays = eventRelayDao.getAllEventRelays()

        // Cover pubkey-write-relay pairing
        val pubkeyCache = mutableSetOf<PubkeyHex>()
        nip65Dao
            .getWriteRelays(pubkeys = pubkeys)
            .groupBy { it.nip65Relay.url }
            .toList()
            .sortedByDescending { (_, pubkeys) -> pubkeys.size }
            .sortedByDescending { (relay, _) -> eventRelays.contains(relay) }
            .sortedByDescending { (relay, _) -> connectedRelays.contains(relay) }
            .sortedByDescending { (relay, _) -> connectionStatuses.value[relay] != Disconnected }
            .take(MAX_RELAY_CONNECTIONS)
            .forEach { (relay, nip65Entities) ->
                val newPubkeys = nip65Entities.map { it.pubkey }.toSet() - pubkeyCache
                if (newPubkeys.isNotEmpty()) {
                    result.putIfAbsent(relay, newPubkeys.toMutableSet())
                    pubkeyCache.addAll(newPubkeys)
                }
            }

        // Cover most useful relays
        eventRelayDao.getEventRelayAuthorView(authors = pubkeys)
            .asSequence()
            .filter { connectionStatuses.value[it.relayUrl] != Disconnected }
            .sortedByDescending { it.relayCount }
            .sortedByDescending { connectedRelays.contains(it.relayUrl) }
            .distinctBy { it.pubkey }
            .groupBy(keySelector = { it.relayUrl }, valueTransform = { it.pubkey })
            .forEach { (relay, pubkeys) ->
                if (pubkeys.isNotEmpty() && (result.containsKey(relay) || result.size < MAX_RELAY_CONNECTIONS)) {
                    result.putOrAdd(relay, pubkeys)
                    pubkeyCache.addAll(pubkeys)
                }
            }

        // Cover rest with already selected relays and read relays for initial start up
        val restPubkeys = pubkeys - pubkeyCache
        if (restPubkeys.isNotEmpty()) {
            Log.w(TAG, "Default to read relays for ${restPubkeys.size}/${pubkeys.size} pubkeys")
            result.keys.forEach { relay -> result.putOrAdd(relay, restPubkeys) }
            getReadRelays().forEach { relay -> result.putOrAdd(relay, restPubkeys) }
        }

        Log.i(TAG, "Selected ${result.size} autopilot relays ${result.keys}")

        nostrClient.addRelays(relayUrls = result.keys)

        return result
    }

    suspend fun getNewestCreatedAt() = nip65Dao.getNewestCreatedAt()

    suspend fun getCreatedAt(pubkey: PubkeyHex) = nip65Dao.getNewestCreatedAt(pubkey = pubkey)

    fun getMyNip65(): List<Nip65Relay> {
        return myNip65.value.map { it.nip65Relay }
            .ifEmpty { defaultRelays.map { Nip65Relay(url = it) } }
    }

    suspend fun getPopularRelays() = nip65Dao.getPopularRelays(limit = MAX_POPULAR_RELAYS)

    private fun List<RelayUrl>.preferConnected(limit: Int): List<RelayUrl> {
        if (this.size <= limit) return this

        val connected = nostrClient.getAllConnectedUrls().toSet()
        return this.shuffled().sortedByDescending { connected.contains(it) }.take(limit)
    }

    private fun List<RelayUrl>.normalize(limit: Int = Int.MAX_VALUE): List<RelayUrl> {
        return this.map { it.removeTrailingSlashes() }
            .distinct()
            .take(limit)
    }

    private val defaultRelays = listOf(
        "wss://nos.lol",
        "wss://nostr.einundzwanzig.space",
        "wss://relay.mutinywallet.com",
        "wss://nostr.fmt.wiz.biz",
        "wss://relay.nostr.wirednet.jp",
    )
}
