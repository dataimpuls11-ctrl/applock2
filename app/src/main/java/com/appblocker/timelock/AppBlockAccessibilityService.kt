package com.appblocker.timelock

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Watches which app comes to the foreground. The moment a blocked package
 * is detected (always-blocked, or inside its scheduled time window), it is
 * closed immediately by sending the device Home and showing a notification —
 * there is no multi-minute delay, it happens on the very first detected event.
 */
class AppBlockAccessibilityService : AccessibilityService() {

    private var lastHandledPackage: String? = null
    private var lastHandledAt: Long = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg == packageName) return // never block ourselves

        if (BlockRepository.isBlockedNow(this, pkg)) {
            // Debounce: avoid spamming Home/notification if several events
            // fire for the same package within a short window.
            val now = System.currentTimeMillis()
            if (pkg == lastHandledPackage && now - lastHandledAt < 1500) return
            lastHandledPackage = pkg
            lastHandledAt = now

            performGlobalAction(GLOBAL_ACTION_HOME)
            BlockNotifier.show(this, pkg)
        }
    }

    override fun onInterrupt() {
        // No-op: required by the AccessibilityService contract.
    }
}
