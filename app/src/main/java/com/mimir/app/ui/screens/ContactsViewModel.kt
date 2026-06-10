package com.mimir.app.ui.screens

import android.app.Application
import androidx.lifecycle.*
import com.mimir.app.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ContactsViewModel(app: Application) : AndroidViewModel(app) {

    private val db = (app as com.mimir.app.MimirApplication).db
    val contacts = db.contacts().allContacts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addContact(pubkeyHex: String, nickname: String) {
        if (pubkeyHex.length != 64) return
        viewModelScope.launch {
            db.contacts().upsert(Contact(pubkeyHex = pubkeyHex, nickname = nickname))
            MimirBridge.sendContactRequest(pubkeyHex, "Привет! Добавляю тебя в контакты.")
        }
    }
}
