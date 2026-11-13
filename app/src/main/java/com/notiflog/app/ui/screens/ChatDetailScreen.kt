package com.notiflog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.notiflog.app.MainViewModel
import com.notiflog.app.database.NotifEntity
import com.notiflog.app.utils.FileManager
import com.notiflog.app.utils.fmtTime
import java.io.File

private fun isImg(t: String?) = t == "image" || t == "sticker"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(vm: MainViewModel, key: String, onBack: () -> Unit) {
    val msgs by remember(key) { vm.messages(key) }.collectAsState(initial = emptyList())
    var preview by remember { mutableStateOf<NotifEntity?>(null) }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(msgs.firstOrNull()?.chatName ?: "Chat", maxLines = 1) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") } }
        )
    }) { pad ->
        val state = rememberLazyListState()
        LaunchedEffect(msgs.size) { if (msgs.isNotEmpty()) state.scrollToItem(msgs.lastIndex) }
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize(), state = state,
            contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) { items(msgs, key = { it.id }) { m -> Bubble(m) { preview = m } } }
    }
    preview?.let { MediaPreviewDialog(it) { preview = null } }
}

@Composable
private fun Bubble(m: NotifEntity, onMedia: () -> Unit) {
    val ctx = LocalContext.current
    val path = m.mediaPath
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.widthIn(max = 320.dp)) {
        Column(Modifier.padding(12.dp)) {
            if (m.isGroup) Text(m.sender, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            if (m.replyText != null) Text("Membalas${m.replyTo?.let { " $it" } ?: ""}: ${m.replyText}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            if (m.viewOnce) Text("Media Sekali Lihat", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            if (path != null) {
                MediaThumb(m, Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 240.dp).clip(RoundedCornerShape(12.dp)).clickable { onMedia() })
                Text(File(path).name, style = MaterialTheme.typography.labelSmall)
                TextButton(onClick = { FileManager.openFolder(ctx, path) }) {
                    Icon(Icons.Filled.Folder, null); Spacer(Modifier.width(6.dp)); Text("Buka di File Manager")
                }
            } else if (m.mediaType != null) {
                Text("(${m.mediaType}) preview tidak tersedia dari notifikasi", style = MaterialTheme.typography.bodySmall)
            }
            if (m.text.isNotBlank()) Text(m.text, style = MaterialTheme.typography.bodyLarge)
            Text(fmtTime(m.time), style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.End))
        }
    }
}

@Composable
fun MediaThumb(m: NotifEntity, modifier: Modifier) {
    val path = m.mediaPath ?: return
    if (isImg(m.mediaType)) {
        AsyncImage(model = File(path), contentDescription = File(path).name, contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surface), Alignment.Center) {
            Icon(if (m.mediaType == "video") Icons.Filled.Videocam else Icons.Filled.Mic, null, Modifier.size(40.dp))
        }
    }
}

@Composable
fun MediaPreviewDialog(m: NotifEntity, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val path = m.mediaPath ?: return
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black).clickable { onClose() }, Alignment.Center) {
            if (isImg(m.mediaType)) AsyncImage(model = File(path), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            else Button(onClick = { FileManager.openFile(ctx, path) }, modifier = Modifier.height(60.dp)) { Text("Buka file") }
            Column(Modifier.align(Alignment.BottomCenter).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(File(path).name, color = Color.White)
                TextButton(onClick = { FileManager.openFolder(ctx, path) }) { Text("Buka di File Manager") }
            }
        }
    }
}
