package com.mimir.app.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import uniffi.mimir.*

object MimirBridge {

    private const val TAG = "MimirBridge"
    private var peerNode: PeerNode? = null

    sealed class Event {
        data class OnlineChanged(val online: Boolean) : Event()
        data class PeerConnected(val pubkeyHex: String) : Event()
        data class PeerDisconnected(val pubkeyHex: String, val dead: Boolean) : Event()
        data class MessageReceived(
            val pubkeyHex: String, val guid: Long, val replyTo: Long,
            val sendTime: Long, val msgType: Int, val data: ByteArray
        ) : Event()
        data class MessageDelivered(val pubkeyHex: String, val guid: Long) : Event()
        data class FileReceived(
            val pubkeyHex: String, val guid: Long, val metaJson: String, val filePath: String
        ) : Event()
        data class IncomingCall(val pubkeyHex: String) : Event()
        data class CallStatusChanged(val status: CallStatus, val pubkeyHex: String?) : Event()
        data class CallPacket(val pubkeyHex: String, val data: ByteArray) : Event()
        data class FileSendProgress(val pubkeyHex: String, val guid: Long, val sent: Long, val total: Long) : Event()
        data class FileReceiveProgress(val pubkeyHex: String, val guid: Long, val received: Long, val total: Long) : Event()
        data class ContactRequest(val pubkeyHex: String, val message: String, val nickname: String) : Event()
        data class ContactResponse(val pubkeyHex: String, val accepted: Boolean) : Event()
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    fun start(
        context: Context,
        seedHex: String,
        yggPeers: List<String>,
        trackers: List<String>,
        filesDir: String,
    ) {
        if (peerNode != null) return

        val signingKey = seedHex.hexToBytes()

        // Сохранённый ephemeral ключ (для стабильного Yggdrasil-адреса)
        val prefs = context.getSharedPreferences("mimir_prefs", Context.MODE_PRIVATE)
        val savedEphemeral = prefs.getString("ephemeral_key_hex", null)?.hexToBytes()

        val listener = object : PeerEventListener {
            override fun onConnectivityChanged(isOnline: Boolean) {
                emit(Event.OnlineChanged(isOnline))
            }
            override fun onPeerConnected(pubkey: ByteArray, address: String) {
                emit(Event.PeerConnected(pubkey.toHex()))
            }
            override fun onPeerDisconnected(pubkey: ByteArray, address: String, deadPeer: Boolean) {
                emit(Event.PeerDisconnected(pubkey.toHex(), deadPeer))
            }
            override fun onMessageReceived(
                pubkey: ByteArray, guid: Long, replyTo: Long,
                sendTime: Long, editTime: Long, msgType: Int, data: ByteArray
            ) {
                emit(Event.MessageReceived(pubkey.toHex(), guid, replyTo, sendTime, msgType, data))
            }
            override fun onMessageDelivered(pubkey: ByteArray, guid: Long) {
                emit(Event.MessageDelivered(pubkey.toHex(), guid))
            }
            override fun onFileReceived(
                pubkey: ByteArray, guid: Long, replyTo: Long, sendTime: Long,
                editTime: Long, msgType: Int, metaJson: String, filePath: String
            ) {
                emit(Event.FileReceived(pubkey.toHex(), guid, metaJson, filePath))
            }
            override fun onIncomingCall(pubkey: ByteArray) {
                emit(Event.IncomingCall(pubkey.toHex()))
            }
            override fun onCallStatusChanged(status: CallStatus, pubkey: ByteArray?) {
                emit(Event.CallStatusChanged(status, pubkey?.toHex()))
            }
            override fun onCallPacket(pubkey: ByteArray, data: ByteArray) {
                emit(Event.CallPacket(pubkey.toHex(), data))
            }
            override fun onFileReceiveProgress(pubkey: ByteArray, guid: Long, bytesReceived: Long, totalBytes: Long) {
                emit(Event.FileReceiveProgress(pubkey.toHex(), guid, bytesReceived, totalBytes))
            }
            override fun onFileSendProgress(pubkey: ByteArray, guid: Long, bytesSent: Long, totalBytes: Long) {
                emit(Event.FileSendProgress(pubkey.toHex(), guid, bytesSent, totalBytes))
            }
            override fun onContactRequest(pubkey: ByteArray, message: String, nickname: String, info: String, avatar: ByteArray?) {
                emit(Event.ContactRequest(pubkey.toHex(), message, nickname))
            }
            override fun onContactResponse(pubkey: ByteArray, accepted: Boolean) {
                emit(Event.ContactResponse(pubkey.toHex(), accepted))
            }
            override fun onTrackerAnnounce(ok: Boolean, ttl: Int) {
                Log.d(TAG, "Tracker announce ok=$ok ttl=$ttl")
            }
        }

        val provider = object : InfoProvider {
            override fun getMyInfo(sinceTime: Long): ContactInfo? = null
            override fun getContactUpdateTime(pubkey: ByteArray): Long = 0L
            override fun updateContactInfo(pubkey: ByteArray, info: ContactInfo) {}
            override fun getFilesDir(): String = filesDir
            override fun getPeerFlags(pubkey: ByteArray): Int = 1
        }

        try {
            val node = PeerNode(
                signingKey    = signingKey,
                ephemeralKey  = savedEphemeral,
                yggPeers      = yggPeers,
                peerPort      = 7878u,
                trackers      = trackers,
                eventListener = listener,
                infoProvider  = provider,
            )
            // Сохраняем ephemeral ключ для следующего запуска
            prefs.edit()
                .putString("ephemeral_key_hex", node.ephemeralKey().toHex())
                .apply()

            node.announceToTrackers()
            peerNode = node
            Log.i(TAG, "PeerNode started, pubkey=${node.publicKey().toHex().take(16)}…")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start PeerNode", e)
        }
    }

    fun stop() { peerNode?.stop(); peerNode = null }

    fun publicKey(): ByteArray? = peerNode?.publicKey()

    fun connectToPeer(pubkeyHex: String) =
        runCatching { peerNode?.connectToPeer(pubkeyHex.hexToBytes()) }

    fun sendMessage(pubkeyHex: String, guid: Long, msgType: Int, data: ByteArray) =
        runCatching {
            peerNode?.sendMessage(pubkeyHex.hexToBytes(), guid, 0L,
                System.currentTimeMillis(), 0L, msgType, data)
        }

    fun sendContactRequest(pubkeyHex: String, message: String) =
        runCatching { peerNode?.sendContactRequest(pubkeyHex.hexToBytes(), message) }

    fun sendContactResponse(pubkeyHex: String, accepted: Boolean) =
        runCatching { peerNode?.sendContactResponse(pubkeyHex.hexToBytes(), accepted) }

    fun startCall(pubkeyHex: String) =
        runCatching { peerNode?.startCall(pubkeyHex.hexToBytes()) }

    fun answerCall(pubkeyHex: String, accept: Boolean) =
        runCatching { peerNode?.answerCall(pubkeyHex.hexToBytes(), accept) }

    fun hangupCall(pubkeyHex: String) =
        runCatching { peerNode?.hangupCall(pubkeyHex.hexToBytes()) }

    fun sendCallPacket(pubkeyHex: String, data: ByteArray) =
        runCatching { peerNode?.sendCallPacket(pubkeyHex.hexToBytes(), data) }

    fun requestFile(pubkeyHex: String, guid: Long, name: String, hash: String, size: Long) =
        runCatching { peerNode?.requestFile(pubkeyHex.hexToBytes(), guid, name, hash, size) }

    fun setNetworkOnline(online: Boolean) = peerNode?.setNetworkOnline(online)

    private fun emit(event: Event) { _events.tryEmit(event) }

    fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    fun String.hexToBytes(): ByteArray {
        val s = if (length % 2 != 0) "0$this" else this
        return ByteArray(s.length / 2) { i ->
            s.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
