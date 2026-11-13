package com.notiflog.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.notiflog.app.MainViewModel
import com.notiflog.app.database.NotifEntity

@Composable
fun MediaScreen(vm: MainViewModel) {
    val media by vm.media.collectAsState()
    var preview by remember { mutableStateOf<NotifEntity?>(null) }
    if (media.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) { Text("Belum ada media tersimpan") }
    } else LazyVerticalGrid(
        columns = GridCells.Fixed(3), contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(media, key = { it.id }) { m -> MediaThumb(m, Modifier.aspectRatio(1f).clickable { preview = m }) }
    }
    preview?.let { MediaPreviewDialog(it) { preview = null } }
}
