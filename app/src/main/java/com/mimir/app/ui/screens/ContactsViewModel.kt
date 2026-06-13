package com.mimir.app.ui.screens

import android.app.Application
import androidx.lifecycle.*
import com.mimir.app.data.*
import com.mimir.app.data.MimirBridge
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class NetworkStatus { CONNECTING, ONLINE, OFFLINE }

data class ConnectionState(
    val status: NetworkStatus = NetworkStatus.CONNECTING,
    val peerName: String = "",   // название пира, к которому подключены
)

class ContactsViewModel(app: Application) : AndroidViewModel(app) {

    private val db = (app as com.mimir.app.MimirApplication).db

    val contacts = db.contacts().allContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _myPubkeyHex = MutableStateFlow("")
    val myPubkeyHex: StateFlow<String> = _myPubkeyHex

    private val _connectionState = MutableStateFlow(ConnectionState())
    val connectionState: StateFlow<ConnectionState> = _connectionState

    // Список Yggdrasil-пиров из ConnectionService для сопоставления адреса
    private val knownPeers = mapOf(
        "de1.mimir.im" to "de1.mimir.im",
        "de2.mimir.im" to "de2.mimir.im",
        "sk1.mimir.im" to "sk1.mimir.im",
        "sk2.mimir.im" to "sk2.mimir.im",
        "us1.mimir.im" to "us1.mimir.im",
    )

    init {
        // Ждём публичный ключ от PeerNode
        viewModelScope.launch {
            while (_myPubkeyHex.value.isEmpty()) {
                val key = MimirBridge.publicKey()
                if (key != null) {
                    _myPubkeyHex.value = with(MimirBridge) { key.toHex() }
                } else {
                    kotlinx.coroutines.delay(500)
                }
            }
        }

        // Слушаем события сети и контактов
        viewModelScope.launch {
            MimirBridge.events.collect { event ->
                when (event) {
                    is MimirBridge.Event.OnlineChanged -> {
                        _connectionState.update { state ->
                            state.copy(
                                status   = if (event.online) NetworkStatus.ONLINE
                                           else NetworkStatus.OFFLINE,
                                peerName = if (event.online) state.peerName else "",
                            )
                        }
                    }
                    is MimirBridge.Event.PeerConnected -> {
                        val peerName = resolvePeerName(event.address)
                        _connectionState.update { it.copy(
                            status   = NetworkStatus.ONLINE,
                            peerName = peerName
                        )}
                        db.contacts().setOnline(event.pubkeyHex, true, System.currentTimeMillis())
                    }
                    is MimirBridge.Event.PeerDisconnected -> {
                        db.contacts().setOnline(event.pubkeyHex, false, System.currentTimeMillis())
                    }
                    is MimirBridge.Event.ContactRequest -> {
                        val existing = db.contacts().byKey(event.pubkeyHex)
                        if (existing == null) {
                            db.contacts().upsert(
                                Contact(
                                    pubkeyHex = event.pubkeyHex,
                                    nickname  = event.nickname.ifEmpty { event.pubkeyHex.take(8) },
                                )
                            )
                        }
                        MimirBridge.sendContactResponse(event.pubkeyHex, true)
                    }
                    else -> {}
                }
            }
        }

        // Начальный статус — подключение
        viewModelScope.launch {
            kotlinx.coroutines.delay(8000)
            // Если через 8 сек всё ещё CONNECTING — значит офлайн
            if (_connectionState.value.status == NetworkStatus.CONNECTING) {
                _connectionState.update { it.copy(status = NetworkStatus.OFFLINE) }
            }
        }
    }

    private fun resolvePeerName(address: String): String {
        // address приходит как IPv6 Yggdrasil адрес или hostname
        // Ищем совпадение с известными пирами
        knownPeers.keys.forEach { host ->
            if (address.contains(host)) return host
        }
        // Если адрес IPv6 — показываем сокращённо
        return if (address.contains(":")) {
            val parts = address.substringBefore("]").removePrefix("[").split(":")
            if (parts.size >= 2) "${parts[0]}:${parts[1]}:…" else address.take(20)
        } else address.take(20)
    }

    fun addContact(pubkeyHex: String, nickname: String) {
        if (pubkeyHex.length != 64) return
        viewModelScope.launch {
            db.contacts().upsert(Contact(pubkeyHex = pubkeyHex, nickname = nickname))
            MimirBridge.sendContactRequest(pubkeyHex, "Привет! Добавляю тебя в контакты.")
        }
    }
}
