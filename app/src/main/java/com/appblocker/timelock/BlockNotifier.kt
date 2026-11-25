package com.appblocker.timelock

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat

object BlockNotifier {

    private const val CHANNEL_ID = "app_blocker_channel"
    private const val CHANNEL_NAME = "App Blocker Alerts"

    fun show(context: Context, blockedPackage: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
                )
                manager.createNotificationChannel(channel)
            }
        }

        val label = try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(blockedPackage, PackageManager.GET_META_DATA)).toString()
        } catch (e: Exception) {
            blockedPackage
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText("$label is blocked right now")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            manager.notify(blockedPackage.hashCode(), notification)
        } catch (e: SecurityException) {
            // Notification permission not granted (Android 13+); the app is
            // still blocked, it just won't show a notification about it.
        }
    }

    /** Shown when a "prevent re-install" app is detected being installed. */
    fun notifyInstallBlocked(context: Context, blockedPackage: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
                )
            }
        }

        val label = try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(blockedPackage, 0)).toString()
        } catch (e: Exception) {
            blockedPackage
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("⛔ Installation Blocked")
            .setContentText("\"$label\" was installed during a blocked period. Please uninstall it.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            manager.notify("install_blocked_${blockedPackage}".hashCode(), notification)
        } catch (e: SecurityException) { /* no notification permission */ }
    }
}
