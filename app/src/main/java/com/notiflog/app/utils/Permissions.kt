package com.notiflog.app.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.NotificationManagerCompat

object Permissions {
    fun listener(ctx: Context) = NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
    fun storage(ctx: Context) = FileManager.hasAccess(ctx)

    fun request(ctx: Context, launcher: ActivityResultLauncher<Array<String>>) {
        val nt = Intent.FLAG_ACTIVITY_NEW_TASK
        if (!listener(ctx)) {
            ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(nt)); return
        }
        if (!storage(ctx)) {
            if (Build.VERSION.SDK_INT >= 30) {
                try {
                    ctx.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${ctx.packageName}")).addFlags(nt))
                } catch (e: Exception) {
                    ctx.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).addFlags(nt))
                }
            } else launcher.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE))
            return
        }
        if (Build.VERSION.SDK_INT >= 33) launcher.launch(arrayOf(
            Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO))
    }
}
