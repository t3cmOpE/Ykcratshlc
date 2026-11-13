package com.notiflog.app.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "notifs", indices = [Index(value = ["uid"], unique = true), Index("chatKey")])
data class NotifEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String,
    val appName: String,
    val pkg: String,
    val chatKey: String,
    val chatName: String,
    val sender: String,
    val phone: String? = null,
    val title: String,
    val text: String,
    val time: Long,
    val isGroup: Boolean = false,
    val groupName: String? = null,
    val replyTo: String? = null,
    val replyText: String? = null,
    val category: String = "chat",
    val mediaType: String? = null,
    val mediaPath: String? = null,
    val viewOnce: Boolean = false
)

data class ChatRow(
    val chatKey: String,
    val name: String,
    val lastText: String,
    val lastTime: Long,
    val count: Int,
    val isGroup: Boolean
)

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val model: String,
    val host: String?,
    val token: String,
    val lastActive: Long
)
