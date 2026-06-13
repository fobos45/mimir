package com.mimir.app.ui.screens

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class YggPeer(
    val id: String,
    val address: String,
    val label: String,
    val enabled: Boolean,
    val isDefault: Boolean = false,
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("mimir_settings", Context.MODE_PRIVATE)

    private val _peers = MutableStateFlow<List<YggPeer>>(emptyList())
    val peers: StateFlow<List<YggPeer>> = _peers

    // true = использовать трекер (ephemeral ключ), false = прямое подключение (постоянный ключ)
    private val _useTracker = MutableStateFlow(prefs.getBoolean("use_tracker", true))
    val useTracker: StateFlow<Boolean> = _useTracker

    init {
        loadPeers()
    }

    fun setUseTracker(enabled: Boolean) {
        _useTracker.value = enabled
        prefs.edit().putBoolean("use_tracker", enabled).apply()
    }

    private fun loadPeers() {
        val json = prefs.getString("ygg_peers", null)
        if (json == null) {
            _peers.value = defaultPeers()
            savePeers()
        } else {
            try {
                val arr = JSONArray(json)
                val list = mutableListOf<YggPeer>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(YggPeer(
                        id        = obj.getString("id"),
                        address   = obj.getString("address"),
                        label     = obj.getString("label"),
                        enabled   = obj.getBoolean("enabled"),
                        isDefault = obj.optBoolean("isDefault", false),
                    ))
                }
                _peers.value = list
            } catch (e: Exception) {
                _peers.value = defaultPeers()
                savePeers()
            }
        }
    }

    fun togglePeer(id: String, enabled: Boolean) {
        _peers.update { list -> list.map { if (it.id == id) it.copy(enabled = enabled) else it } }
        savePeers()
    }

    fun addPeer(address: String, label: String) {
        if (address.isBlank()) return
        val id = "custom_${System.currentTimeMillis()}"
        _peers.update { it + YggPeer(id = id, address = address, label = label.ifBlank { address }, enabled = true) }
        savePeers()
    }

    fun removePeer(id: String) {
        _peers.update { list -> list.filter { it.id != id || it.isDefault } }
        savePeers()
    }

    fun enabledPeerAddresses(): List<String> =
        _peers.value.filter { it.enabled }.map { it.address }

    private fun savePeers() {
        val arr = JSONArray()
        _peers.value.forEach { peer ->
            arr.put(JSONObject().apply {
                put("id",        peer.id)
                put("address",   peer.address)
                put("label",     peer.label)
                put("enabled",   peer.enabled)
                put("isDefault", peer.isDefault)
            })
        }
        prefs.edit().putString("ygg_peers", arr.toString()).apply()
    }

    private fun defaultPeers() = listOf(
        YggPeer("de1", "tcp://de1.mimir.im:7743", "Германия 1 (de1)", true,  true),
        YggPeer("de2", "tcp://de2.mimir.im:7743", "Германия 2 (de2)", true,  true),
        YggPeer("sk1", "tcp://sk1.mimir.im:7743", "Словакия 1 (sk1)", true,  true),
        YggPeer("sk2", "tcp://sk2.mimir.im:7743", "Словакия 2 (sk2)", false, true),
        YggPeer("us1", "tcp://us1.mimir.im:7743", "США 1 (us1)",      false, true),
    )
}
