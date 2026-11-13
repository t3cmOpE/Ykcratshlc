package com.notiflog.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notiflog.app.database.*
import com.notiflog.app.utils.SyncManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(private val app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).dao()
    val query = MutableStateFlow("")
    val filter = MutableStateFlow(0)
    val info = MutableStateFlow("")

    val chats = query.combine(filter) { q, f -> q to f }
        .flatMapLatest { dao.chats(it.first.trim(), it.second) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val media = dao.media().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val devices = dao.devices().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun messages(key: String) = dao.messages(key)
    fun clearAll() { viewModelScope.launch { dao.clear() } }
    fun disconnect(d: DeviceEntity) { viewModelScope.launch { dao.deleteDevice(d.id) } }

    fun pairWithCode(code: String) {
        viewModelScope.launch {
            info.value = "Mencari HP utama…"
            val host = SyncManager.discover(code.trim())
            if (host == null) { info.value = "HP utama tidak ditemukan. Cek WiFi & kode."; return@launch }
            info.value = "Menunggu izin dari HP utama…"
            info.value = SyncManager.pair(app, host, code.trim())
        }
    }

    fun pairWithQr(raw: String) {
        val u = Uri.parse(raw)
        val h = u.getQueryParameter("h"); val c = u.getQueryParameter("c")
        if (h == null || c == null) { info.value = "QR tidak dikenali"; return }
        viewModelScope.launch {
            info.value = "Menunggu izin dari HP utama…"
            info.value = SyncManager.pair(app, h, c)
        }
    }

    fun sync(d: DeviceEntity) {
        viewModelScope.launch { info.value = "Menyinkronkan…"; info.value = SyncManager.pull(app, d) }
    }
}
