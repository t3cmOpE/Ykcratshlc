package com.notiflog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notiflog.app.ui.screens.*
import com.notiflog.app.ui.theme.NotifLogTheme
import com.notiflog.app.utils.Permissions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NotifLogTheme { Surface(color = MaterialTheme.colorScheme.background) { Root() } } }
    }
}

@Composable
fun Root() {
    val vm: MainViewModel = viewModel()
    val ctx = LocalContext.current
    var showPerm by remember { mutableStateOf(!(Permissions.listener(ctx) && Permissions.storage(ctx))) }
    var tab by remember { mutableIntStateOf(0) }
    var chat by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = chat != null) { chat = null }
    val open = chat
    if (showPerm) {
        PermissionScreen { showPerm = false }
    } else if (open != null) {
        ChatDetailScreen(vm, open) { chat = null }
    } else {
        val items = listOf("Chat" to Icons.Filled.Chat, "Media" to Icons.Filled.Photo, "Perangkat" to Icons.Filled.Devices, "Pengaturan" to Icons.Filled.Settings)
        Scaffold(bottomBar = {
            NavigationBar {
                items.forEachIndexed { i, (label, icon) ->
                    NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Icon(icon, label) }, label = { Text(label) })
                }
            }
        }) { pad ->
            androidx.compose.foundation.layout.Box(Modifier.padding(pad)) {
                when (tab) {
                    0 -> ChatListScreen(vm) { chat = it }
                    1 -> MediaScreen(vm)
                    2 -> DevicesScreen(vm)
                    else -> SettingsScreen(vm) { showPerm = true }
                }
            }
        }
    }
}
