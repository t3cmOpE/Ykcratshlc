package com.notiflog.app.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

fun fmtTime(t: Long): String {
    val now = Calendar.getInstance()
    val c = Calendar.getInstance().apply { timeInMillis = t }
    val same = now.get(Calendar.YEAR) == c.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR)
    return SimpleDateFormat(if (same) "HH:mm" else "dd/MM HH:mm", Locale.getDefault()).format(Date(t))
}

object FileManager {
    val root: File get() = File(Environment.getExternalStorageDirectory(), "NotifLog")
    private val dirs = listOf(
        "media", "WhatsApp", "WhatsApp/Chat", "WhatsApp/Media",
        "WhatsApp/Media/Images", "WhatsApp/Media/Videos", "WhatsApp/Media/Audio"
    )

    fun hasAccess(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager()
        else ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    fun ensureDirs(): Boolean = try {
        dirs.all { val d = File(root, it); d.exists() || d.mkdirs() }
    } catch (e: Exception) { false }

    fun uniqueFile(dir: File, base: String, ext: String): File {
        var f = File(dir, "$base.$ext")
        var i = 1
        while (f.exists()) { f = File(dir, "${base}_$i.$ext"); i++ }
        return f
    }

    fun appendChat(chatName: String, line: String) {
        try {
            ensureDirs()
            val safe = chatName.replace(Regex("[^A-Za-z0-9_.-]"), "_").take(60).ifEmpty { "chat" }
            File(root, "WhatsApp/Chat/$safe.txt").appendText(line + "\n")
        } catch (e: Exception) { }
    }

    fun openFolder(ctx: Context, path: String) {
        try {
            val rel = File(path).parentFile!!.absolutePath.removePrefix(Environment.getExternalStorageDirectory().absolutePath).trim('/')
            val uri = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:$rel")
            ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "vnd.android.document/directory").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(ctx, "Buka manual: ${File(path).parent}", Toast.LENGTH_LONG).show()
        }
    }

    fun openFile(ctx: Context, path: String) {
        try {
            val f = File(path)
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(f.extension.lowercase()) ?: "*/*"
            ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(ctx, "Tidak ada aplikasi untuk membuka file ini", Toast.LENGTH_SHORT).show()
        }
    }
}
