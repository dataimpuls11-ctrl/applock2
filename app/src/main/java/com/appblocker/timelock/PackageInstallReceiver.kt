package com.appblocker.timelock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Listens for ACTION_PACKAGE_ADDED broadcasts.
 *
 * If the newly installed package has "prevent re-installation" enabled
 * AND the app is currently in a blocked period, we immediately launch
 * the UninstallBlockedActivity which asks the user to uninstall it again,
 * and shows a notification.
 *
 * Note: Android does NOT allow apps to silently block an install.
 * The best we can do is detect the install immediately after it completes
 * and prompt/force the user to uninstall.  This is the same approach
 * used by enterprise MDM tools on non-rooted devices.
 */
class PackageInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_PACKAGE_ADDED) return

        val pkg = intent.data?.schemeSpecificPart ?: return
        val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)

        // We only care about fresh installs, not updates of already-installed apps.
        if (isReplacing) return

        val shouldBlock = BlockRepository.shouldPreventReinstall(context, pkg)
                && BlockRepository.isBlockedNow(context, pkg)

        if (shouldBlock) {
            // Show a toast immediately.
            Toast.makeText(
                context,
                "⛔ \"$pkg\" was blocked from being installed. Please uninstall it.",
                Toast.LENGTH_LONG
            ).show()

            // Fire a notification so the user sees it even if they're not watching.
            BlockNotifier.notifyInstallBlocked(context, pkg)

            // Launch the uninstall prompt screen.
            val uninstallIntent = Intent(context, UninstallBlockedActivity::class.java).apply {
                putExtra(UninstallBlockedActivity.EXTRA_PACKAGE, pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(uninstallIntent)
        }
    }
}
