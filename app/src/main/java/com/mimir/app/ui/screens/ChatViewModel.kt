package com.mimir.app.ui.screens

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.mimir.app.data.*
import com.mimir.app.data.MimirBridge.toHex
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class ChatViewModel(app: Application, val contactPubkey: String) : AndroidViewModel(app) {

    private val db = (app as com.mimir.app.MimirApplication).db

    // WhileSubscribed чтобы Flow активно работал пока экран открыт
    val messages = db.messages().forContact(contactPubkey)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contact = db.contacts().allContacts()
        .map { list -> list.find { it.pubkeyHex == contactPubkey } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        MimirBridge.connectToPeer(contactPubkey)
        viewModelScope.launch { db.contacts().clearUnread(contactPubkey) }

        // Слушаем входящие сообщения и сразу пишем в БД
        // (дублирует ConnectionService, но гарантирует обновление пока чат открыт)
        viewModelScope.launch {
            MimirBridge.events.collect { event ->
                when (event) {
                    is MimirBridge.Event.MessageReceived -> {
                        if (event.pubkeyHex == contactPubkey) {
                            val text = if (event.msgType == 1)
                                String(event.data, Charsets.UTF_8) else ""
                            db.messages().upsert(
                                Message(
                                    guid          = event.guid,
                                    contactPubkey = contactPubkey,
                                    isOutgoing    = false,
                                    msgType       = event.msgType,
                                    text          = text,
                                    sendTime      = event.sendTime,
                                )
                            )
                            // Сбрасываем счётчик непрочитанных — чат открыт
                            db.contacts().clearUnread(contactPubkey)
                        }
                    }
                    is MimirBridge.Event.MessageDelivered -> {
                        db.messages().markDelivered(event.guid)
                    }
                    is MimirBridge.Event.FileSendProgress -> {
                        db.messages().updateUploadProgress(
                            event.guid, event.sent.toFloat() / event.total.coerceAtLeast(1)
                        )
                    }
                    is MimirBridge.Event.FileReceiveProgress -> {
                        db.messages().updateDownloadProgress(
                            event.guid, event.received.toFloat() / event.total.coerceAtLeast(1)
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun sendText(text: String) {
        if (text.isBlank()) return
        val guid = System.currentTimeMillis()
        val data = text.trim().toByteArray(Charsets.UTF_8)
        MimirBridge.sendMessage(contactPubkey, guid, 1, data)
        viewModelScope.launch {
            db.messages().upsert(
                Message(
                    guid          = guid,
                    contactPubkey = contactPubkey,
                    isOutgoing    = true,
                    msgType       = 1,
                    text          = text.trim(),
                    sendTime      = guid,
                )
            )
        }
    }

    fun sendFile(uri: Uri) {
        val guid = System.currentTimeMillis()
        val cr   = getApplication<Application>().contentResolver
        val name = uri.lastPathSegment ?: "file_$guid"
        val size = cr.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L

        val tmp = File(getApplication<Application>().cacheDir, "send_$guid")
        cr.openInputStream(uri)?.use { inp -> tmp.outputStream().use { inp.copyTo(it) } }

        val data = """{"name":"$name","size":$size,"path":"${tmp.absolutePath}"}"""
            .toByteArray(Charsets.UTF_8)

        MimirBridge.sendMessage(contactPubkey, guid, 2, data)
        viewModelScope.launch {
            db.messages().upsert(
                Message(
                    guid           = guid,
                    contactPubkey  = contactPubkey,
                    isOutgoing     = true,
                    msgType        = 2,
                    fileName       = name,
                    fileSize       = size,
                    sendTime       = guid,
                    uploadProgress = 0f,
                )
            )
        }
    }
}

class ChatViewModelFactory(
    private val app: Application,
    private val pubkey: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ChatViewModel(app, pubkey) as T
    }
}
