package com.notiflog.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object MediaSaver {
    private val prefix = mapOf("image" to "IMG", "video" to "VID", "audio" to "PT", "sticker" to "STK")
    private val ext = mapOf("image" to "jpg", "video" to "mp4", "audio" to "opus", "sticker" to "webp")

    /** Nomor pengirim; kalau tidak ada pakai nama kontak tanpa spasi/simbol. */
    fun idFor(phone: String?, name: String): String =
        phone?.filter { it.isDigit() }?.takeIf { it.length >= 6 }
            ?: name.filter { it.isLetterOrDigit() }.ifEmpty { "Unknown" }

    private fun dirFor(kind: String, isWa: Boolean): File {
        val r = FileManager.root
        if (!isWa) return File(r, "media")
        return when (kind) {
            "video" -> File(r, "WhatsApp/Media/Videos")
            "audio" -> File(r, "WhatsApp/Media/Audio")
            else -> File(r, "WhatsApp/Media/Images")
        }
    }

    /** Format: [nomor]_[PREFIX-tanggal]_[jam].ext, contoh 08123456789_IMG-20261113_210512.jpg */
    fun save(ctx: Context, id: String, kind: String, time: Long, isWa: Boolean, uri: Uri?, bmp: Bitmap?): File? {
        if (!FileManager.hasAccess(ctx)) return null
        return try {
            FileManager.ensureDirs()
            val dir = dirFor(kind, isWa).also { it.mkdirs() }
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(time))
            val e = if (uri == null) "jpg" else ext[kind] ?: "bin"
            val f = FileManager.uniqueFile(dir, "${id}_${prefix[kind] ?: "FILE"}-$ts", e)
            if (uri != null) {
                val ins = ctx.contentResolver.openInputStream(uri) ?: return null
                ins.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
            } else if (bmp != null) {
                f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            } else return null
            f
        } catch (e: Exception) { null }
    }
}
