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
    val messages  = db.messages().forContact(contactPubkey).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val contact   = db.contacts().allContacts()
        .map { list -> list.find { it.pubkeyHex == contactPubkey } }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
        MimirBridge.connectToPeer(contactPubkey)
        viewModelScope.launch { db.contacts().clearUnread(contactPubkey) }
    }

    fun sendText(text: String) {
        if (text.isBlank()) return
        val guid = System.currentTimeMillis()
        val data = text.trim().toByteArray(Charsets.UTF_8)
        MimirBridge.sendMessage(contactPubkey, guid, 1, data)
        viewModelScope.launch {
            db.messages().upsert(Message(
                guid          = guid,
                contactPubkey = contactPubkey,
                isOutgoing    = true,
                msgType       = 1,
                text          = text.trim(),
                sendTime      = guid,
            ))
        }
    }

    fun sendFile(uri: Uri) {
        val guid = System.currentTimeMillis()
        val cr   = getApplication<Application>().contentResolver
        val name = uri.lastPathSegment ?: "file_$guid"
        val size = cr.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L

        // Сохраняем во временный файл и передаём через data stream
        val tmp = File(getApplication<Application>().cacheDir, "send_$guid")
        cr.openInputStream(uri)?.use { inp -> tmp.outputStream().use { inp.copyTo(it) } }

        val data = """{"name":"$name","size":$size,"path":"${tmp.absolutePath}"}"""
            .toByteArray(Charsets.UTF_8)

        MimirBridge.sendMessage(contactPubkey, guid, 2, data)
        viewModelScope.launch {
            db.messages().upsert(Message(
                guid          = guid,
                contactPubkey = contactPubkey,
                isOutgoing    = true,
                msgType       = 2,
                fileName      = name,
                fileSize      = size,
                sendTime      = guid,
                uploadProgress = 0f,
            ))
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
