package com.notiflog.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notiflog.app.MainViewModel

@Composable
fun SettingsScreen(vm: MainViewModel, onPermissions: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Pengaturan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Izin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        PermissionRows(0)
        Button(onClick = onPermissions, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Atur Izin") }
        HorizontalDivider()
        Text("Lokasi file: /storage/emulated/0/NotifLog/\nSemua data hanya disimpan di HP ini.")
        Button(
            onClick = { confirm = true }, modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) { Text("Hapus Semua") }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Hapus semua riwayat?") },
        text = { Text("Semua notifikasi di database akan dihapus. File media di folder NotifLog tidak ikut terhapus.") },
        confirmButton = { TextButton(onClick = { vm.clearAll(); confirm = false }) { Text("Hapus") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Batal") } }
    )
}
