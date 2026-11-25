package com.appblocker.timelock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Accessibility services are restarted automatically by the system once the
 * user has enabled them in Settings, so this receiver doesn't need to do
 * anything special — it's kept as a hook in case future versions need to
 * run setup logic right after boot.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // No-op for now.
        }
    }
}
