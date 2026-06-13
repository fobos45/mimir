package com.mimir.app.ui.screens

import android.app.Application
import androidx.lifecycle.*
import com.mimir.app.data.*
import com.mimir.app.data.MimirBridge
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ContactsViewModel(app: Application) : AndroidViewModel(app) {

    private val db = (app as com.mimir.app.MimirApplication).db

    val contacts = db.contacts().allContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Публичный ключ как StateFlow — обновляется когда PeerNode стартует
    private val _myPubkeyHex = MutableStateFlow("")
    val myPubkeyHex: StateFlow<String> = _myPubkeyHex

    init {
        // Опрашиваем ключ каждые 500мс пока не получим
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

        // Обрабатываем входящие события (онлайн-статус, запросы контактов)
        viewModelScope.launch {
            MimirBridge.events.collect { event ->
                when (event) {
                    is MimirBridge.Event.ContactRequest -> {
                        // Автоматически принимаем запрос и добавляем в контакты
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
                    is MimirBridge.Event.PeerConnected ->
                        db.contacts().setOnline(event.pubkeyHex, true, System.currentTimeMillis())
                    is MimirBridge.Event.PeerDisconnected ->
                        db.contacts().setOnline(event.pubkeyHex, false, System.currentTimeMillis())
                    else -> {}
                }
            }
        }
    }

    fun addContact(pubkeyHex: String, nickname: String) {
        if (pubkeyHex.length != 64) return
        viewModelScope.launch {
            db.contacts().upsert(Contact(pubkeyHex = pubkeyHex, nickname = nickname))
            MimirBridge.sendContactRequest(pubkeyHex, "Привет! Добавляю тебя в контакты.")
        }
    }
}
