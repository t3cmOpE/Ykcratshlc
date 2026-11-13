package com.notiflog.app.ui.screens

import android.graphics.Bitmap
import android.graphics.Color as AColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.notiflog.app.MainViewModel
import com.notiflog.app.utils.SyncManager
import com.notiflog.app.utils.fmtTime

private fun qrBitmap(text: String): Bitmap {
    val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 600, 600)
    val b = Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888)
    for (x in 0 until 600) for (y in 0 until 600) b.setPixel(x, y, if (m[x, y]) AColor.BLACK else AColor.WHITE)
    return b
}

@Composable
fun DevicesScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val devices by vm.devices.collectAsState()
    val info by vm.info.collectAsState()
    val code by SyncManager.code.collectAsState()
    val qr by SyncManager.qr.collectAsState()
    val req by SyncManager.request.collectAsState()
    var input by remember { mutableStateOf("") }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Perangkat Tertaut", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { Text("Hubungkan 2 HP milikmu sendiri di WiFi yang sama. Data tidak lewat internet/server.") }
        item { Text("HP utama", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) }
        item {
            if (code == null) Button(onClick = { SyncManager.startHost(ctx) }, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Tampilkan QR & Kode") }
            else Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                qr?.let { Image(qrBitmap(it).asImageBitmap(), "QR pairing", Modifier.size(220.dp)) }
                Text(code!!, fontSize = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = 6.sp)
                OutlinedButton(onClick = { SyncManager.stopHost() }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Berhenti") }
            }
        }
        item { HorizontalDivider() }
        item { Text("HP kedua", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) }
        item {
            Button(onClick = {
                GmsBarcodeScanning.getClient(ctx).startScan()
                    .addOnSuccessListener { b -> b.rawValue?.let { vm.pairWithQr(it) } }
                    .addOnFailureListener { vm.info.value = "Scan dibatalkan / gagal" }
            }, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Scan Kode QR") }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(input, { input = it.filter { c -> c.isDigit() }.take(6) }, Modifier.weight(1f), label = { Text("Kode 6 digit") }, singleLine = true)
                Button(onClick = { vm.pairWithCode(input) }, enabled = input.length == 6, modifier = Modifier.height(56.dp)) { Text("Hubungkan") }
            }
        }
        if (info.isNotBlank()) item { Text(info, color = MaterialTheme.colorScheme.primary) }
        item { HorizontalDivider() }
        item { Text("Perangkat terhubung", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) }
        if (devices.isEmpty()) item { Text("Belum ada perangkat tertaut") }
        items(devices, key = { it.id }) { d ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(d.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Model: ${d.model}\nTerakhir aktif: ${fmtTime(d.lastActive)}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (d.host != null) Button(onClick = { vm.sync(d) }) { Text("Sinkronkan") }
                        OutlinedButton(onClick = { vm.disconnect(d) }) { Text("Putuskan") }
                    }
                }
            }
        }
    }
    req?.let { r ->
        AlertDialog(
            onDismissRequest = { r.decision.complete(false) },
            title = { Text("Izinkan perangkat ini?") },
            text = { Text("${r.name} (${r.model}) ingin menautkan dan menyalin riwayat NotifLog dari HP ini.") },
            confirmButton = { TextButton(onClick = { r.decision.complete(true) }) { Text("Izinkan") } },
            dismissButton = { TextButton(onClick = { r.decision.complete(false) }) { Text("Tolak") } }
        )
    }
}
