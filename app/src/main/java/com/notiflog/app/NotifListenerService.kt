package com.notiflog.app

import android.app.Notification
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.graphics.drawable.toBitmap
import com.notiflog.app.database.AppDb
import com.notiflog.app.database.NotifEntity
import com.notiflog.app.utils.FileManager
import com.notiflog.app.utils.MediaSaver
import kotlinx.coroutines.*
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class NotifListenerService : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jidRegex = Regex("(\\d{6,15})@s\\.whatsapp\\.net")

    private data class Msg(val sender: String, val text: String, val time: Long, val uri: Uri?, val mime: String?)

    override fun onListenerConnected() { FileManager.ensureDirs() }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try { handle(sbn) } catch (e: Exception) { }
    }

    private fun sha(s: String) = MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun kindOf(t: String): String? {
        val l = t.lowercase()
        return when {
            "sekali lihat" in l || "view once" in l -> "viewonce"
            "sticker" in l || "stiker" in l -> "sticker"
            "voice message" in l || "pesan suara" in l -> "audio"
            "photo" in l || "foto" in l || "📷" in t || "gambar" in l -> "image"
            "video" in l || "🎥" in t -> "video"
            "audio" in l || "🎵" in t -> "audio"
            else -> null
        }
    }

    @Suppress("DEPRECATION")
    private fun handle(sbn: StatusBarNotification) {
        val n = sbn.notification ?: return
        val pkg = sbn.packageName
        if (pkg == packageName) return
        if ((n.flags and Notification.FLAG_GROUP_SUMMARY) != 0 || (n.flags and Notification.FLAG_ONGOING_EVENT) != 0) return
        val ex = n.extras ?: return
        val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (ex.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: ex.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return
        val isWa = pkg.startsWith("com.whatsapp")
        if (isWa && (title == "WhatsApp" || Regex("^\\d+ (new )?messages?.*").containsMatchIn(text) ||
                text.contains("Checking for new messages", true))) return

        val app = try { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString() } catch (e: Exception) { pkg }
        val convTitle = ex.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
        val isGroup = ex.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION) || !convTitle.isNullOrBlank()
        val chatName = (if (!convTitle.isNullOrBlank()) convTitle else title)
            .replace(Regex("\\s*\\(\\d+ (new )?messages?\\)$"), "").ifBlank { app }

        val shortcut = if (Build.VERSION.SDK_INT >= 26) n.shortcutId else null
        var phone = jidRegex.find(sbn.tag.orEmpty())?.groupValues?.get(1) ?: jidRegex.find(shortcut.orEmpty())?.groupValues?.get(1)
        if (phone == null && !isGroup && Regex("^\\+?[\\d\\s-]{8,}$").matches(chatName)) phone = chatName.filter { it.isDigit() }

        val raw = ex.getParcelableArray(Notification.EXTRA_MESSAGES)
        val msgs = if (raw != null) Notification.MessagingStyle.Message.getMessagesFromBundleArray(raw) else emptyList()
        val list: List<Msg> = if (msgs.isNotEmpty()) msgs.map {
            val s = if (Build.VERSION.SDK_INT >= 28) it.senderPerson?.name?.toString() else it.sender?.toString()
            Msg(s ?: title, it.text?.toString().orEmpty(), it.timestamp, it.dataUri, it.dataMimeType)
        } else {
            var s = title; var t = text
            if (isGroup && text.contains(": ")) { s = text.substringBefore(": "); t = text.substringAfter(": ") }
            listOf(Msg(s, t, sbn.postTime, null, null))
        }

        // Bitmap preview dari notifikasi (BigPicture) — harus diambil sekarang
        var pic: Bitmap? = ex.get(Notification.EXTRA_PICTURE) as? Bitmap
        if (pic == null) try { pic = (ex.get("android.pictureIcon") as? Icon)?.loadDrawable(this)?.toBitmap() } catch (e: Exception) { }

        val dao = AppDb.get(applicationContext).dao()
        scope.launch {
            list.forEachIndexed { idx, m ->
                if (m.text.isBlank() && m.uri == null) return@forEachIndexed
                var kind: String? = m.mime?.let {
                    when {
                        it.startsWith("image/") -> "image"; it.startsWith("video/") -> "video"
                        it.startsWith("audio/") -> "audio"; else -> null
                    }
                }
                val label = kindOf(m.text)
                val bmp = if (idx == list.lastIndex) pic else null
                val viewOnce = isWa && label == "viewonce"
                if (kind == null && isWa && label != null && label != "viewonce" && m.text.length <= 40) kind = label
                if (kind == null && bmp != null) kind = "image"
                val isStatus = isWa && !isGroup && m.text.length <= 40 && Regex("(?i)\\b(status|story)\\b").containsMatchIn(m.text + " " + title)
                val chatKey = "$pkg|$chatName"
                val uid = sha(if (msgs.isNotEmpty()) "$chatKey|${m.sender}|${m.text}|${m.time}" else "$chatKey|${m.text}|${m.time / 60000}")
                val e = NotifEntity(
                    uid = uid, appName = app, pkg = pkg, chatKey = chatKey, chatName = chatName, sender = m.sender,
                    phone = phone, title = title, text = m.text, time = m.time, isGroup = isGroup,
                    groupName = if (isGroup) chatName else null, category = if (isStatus) "status" else "chat",
                    mediaType = kind, viewOnce = viewOnce
                )
                val id = dao.insert(e)
                if (id == -1L) return@forEachIndexed
                if (kind != null) {
                    val f = MediaSaver.save(applicationContext, MediaSaver.idFor(phone, if (isGroup) m.sender else chatName), kind, m.time, isWa, m.uri, bmp)
                    if (f != null) dao.setMedia(id, f.absolutePath, kind)
                }
                if (isWa && FileManager.hasAccess(applicationContext)) {
                    val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(m.time))
                    FileManager.appendChat(chatName, "[$ts] ${m.sender}: ${m.text}")
                }
            }
        }
    }
}
