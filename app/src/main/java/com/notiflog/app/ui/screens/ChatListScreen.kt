package com.notiflog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notiflog.app.MainViewModel
import com.notiflog.app.database.ChatRow
import com.notiflog.app.utils.fmtTime

@Composable
fun ChatListScreen(vm: MainViewModel, onOpen: (String) -> Unit) {
    val chats by vm.chats.collectAsState()
    val q by vm.query.collectAsState()
    val f by vm.filter.collectAsState()
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = q, onValueChange = { vm.query.value = it },
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            placeholder = { Text("Cari pesan / pengirim") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true, shape = RoundedCornerShape(28.dp)
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(listOf("Semua", "WhatsApp", "Status", "Media")) { i, l ->
                FilterChip(selected = f == i, onClick = { vm.filter.value = i }, label = { Text(l) })
            }
        }
        Spacer(Modifier.height(8.dp))
        if (chats.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
                Text("Belum ada notifikasi tersimpan.\nPesan yang masuk akan muncul di sini.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else LazyColumn {
            items(chats, key = { it.chatKey }) { c -> ChatItem(c) { onOpen(c.chatKey) } }
        }
    }
}

@Composable
private fun ChatItem(c: ChatRow, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(52.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF2563EB)))),
            Alignment.Center
        ) { Text(c.name.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(c.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Text(c.lastText, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(fmtTime(c.lastTime), style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Badge { Text("${c.count}") }
        }
    }
}
