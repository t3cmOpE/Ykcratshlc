package com.notiflog.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.notiflog.app.utils.Permissions

@Composable
fun PermissionRows(tick: Int) {
    val ctx = LocalContext.current
    val l = remember(tick) { Permissions.listener(ctx) }
    val s = remember(tick) { Permissions.storage(ctx) }
    PermRow("Akses Notifikasi", "Agar NotifLog bisa membaca & menyimpan notifikasi yang masuk.", l)
    PermRow("Akses Penyimpanan", "Agar media tersimpan di /storage/emulated/0/NotifLog.", s)
}

@Composable
private fun PermRow(title: String, desc: String, ok: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (ok) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null,
            tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(12.dp))
        Column { Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium); Text(desc) }
    }
}

@Composable
fun PermissionScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    var tick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val o = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(o); onDispose { owner.lifecycle.removeObserver(o) }
    }
    val ready = remember(tick) { Permissions.listener(ctx) && Permissions.storage(ctx) }
    LaunchedEffect(ready) { if (ready) onDone() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).statusBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Selamat datang di NotifLog", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("NotifLog menyimpan notifikasi dan medianya di HP kamu sendiri. Tidak ada data yang dikirim ke server.")
        PermissionRows(tick)
        Text("Catatan Android 13+: jika tombol Akses Notifikasi abu-abu, buka Info Aplikasi NotifLog → menu ⋮ → \"Izinkan setelan terbatas\", lalu ulangi.",
            style = MaterialTheme.typography.bodySmall)
        Button(onClick = { Permissions.request(ctx, launcher) }, modifier = Modifier.fillMaxWidth().height(60.dp)) { Text("Aktifkan Izin") }
        TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Lewati dulu") }
    }
}
