package com.appblocker.timelock

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Full-screen prompt shown when a "prevent re-install" app is detected
 * being installed during its blocked period.
 *
 * The user MUST uninstall the app — the Back button is disabled so they
 * cannot dismiss this screen without acting.
 */
class UninstallBlockedActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
    }

    private var targetPackage: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        targetPackage = intent.getStringExtra(EXTRA_PACKAGE) ?: run {
            finish()
            return
        }

        setContentView(R.layout.activity_uninstall_blocked)

        val appLabel = getAppLabel(targetPackage)

        findViewById<TextView>(R.id.tvBlockedAppName).text =
            "\"$appLabel\" is blocked and cannot be installed right now."

        findViewById<Button>(R.id.btnUninstall).setOnClickListener {
            launchUninstaller(targetPackage)
        }
    }

    // Prevent back-button dismissal — user must uninstall.
    override fun onBackPressed() {
        // Do nothing — force the user to use the Uninstall button.
    }

    override fun onResume() {
        super.onResume()
        // If the app was uninstalled while we were showing the system uninstaller,
        // close this screen automatically.
        if (!isPackageInstalled(targetPackage)) {
            finish()
        }
    }

    private fun launchUninstaller(pkg: String) {
        val uri = Uri.fromParts("package", pkg, null)
        val uninstallIntent = Intent(Intent.ACTION_DELETE, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(uninstallIntent)
    }

    private fun isPackageInstalled(pkg: String): Boolean {
        return try {
            packageManager.getPackageInfo(pkg, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getAppLabel(pkg: String): String {
        return try {
            val ai = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(ai).toString()
        } catch (e: Exception) {
            pkg
        }
    }
}
