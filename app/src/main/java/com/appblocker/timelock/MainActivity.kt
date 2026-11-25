package com.appblocker.timelock

import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppListAdapter
    private lateinit var statusText: TextView
    private lateinit var statusDot: android.view.View
    private lateinit var statusCard: androidx.cardview.widget.CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        statusText = findViewById(R.id.statusText)
        statusDot = findViewById(R.id.statusDot)
        statusCard = findViewById(R.id.statusCard)

        recyclerView.layoutManager = LinearLayoutManager(this)
        val apps = loadLaunchableApps()
        adapter = AppListAdapter(this, apps) { appInfo -> showScheduleDialog(appInfo) }
        recyclerView.adapter = adapter

        statusCard.setOnClickListener {
            if (!isAccessibilityServiceEnabled()) openAccessibilitySettings()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatusBanner()
        if (!isAccessibilityServiceEnabled()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.permission_needed_title)
                .setMessage(R.string.permission_needed_body)
                .setPositiveButton(R.string.enable_accessibility) { _, _ -> openAccessibilitySettings() }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun updateStatusBanner() {
        val enabled = isAccessibilityServiceEnabled()
        statusText.setText(if (enabled) R.string.service_status_on else R.string.service_status_off)
        statusDot.setBackgroundResource(
            if (enabled) R.drawable.dot_shape_success else R.drawable.dot_shape
        )
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        Toast.makeText(
            this,
            "Find 'App Blocker' in the list and turn it ON",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedId = "$packageName/${AppBlockAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expectedId, ignoreCase = true)) return true
        }
        return false
    }

    private fun loadLaunchableApps(): List<AppInfo> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        return resolveInfos
            .filter { it.activityInfo.packageName != packageName } // don't let it block itself
            .map {
                AppInfo(
                    packageName = it.activityInfo.packageName,
                    label = it.loadLabel(pm).toString(),
                    icon = it.loadIcon(pm)
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    private fun showScheduleDialog(appInfo: AppInfo) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_schedule, null)
        val appNameView = view.findViewById<TextView>(R.id.dialogAppName)
        val btnStart = view.findViewById<Button>(R.id.btnStartTime)
        val btnEnd = view.findViewById<Button>(R.id.btnEndTime)

        appNameView.text = appInfo.label

        val existing = BlockRepository.getSchedules(this)[appInfo.packageName]
        var startMinute = existing?.startMinute ?: (22 * 60)
        var endMinute = existing?.endMinute ?: (6 * 60)

        fun fmt(m: Int) = String.format("%02d:%02d", m / 60, m % 60)
        btnStart.text = fmt(startMinute)
        btnEnd.text = fmt(endMinute)

        btnStart.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                startMinute = h * 60 + m
                btnStart.text = fmt(startMinute)
            }, startMinute / 60, startMinute % 60, true).show()
        }

        btnEnd.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                endMinute = h * 60 + m
                btnEnd.text = fmt(endMinute)
            }, endMinute / 60, endMinute % 60, true).show()
        }

        MaterialAlertDialogBuilder(this)
            .setView(view)
            .setPositiveButton(R.string.save) { _, _ ->
                BlockRepository.setSchedule(this, appInfo.packageName, Schedule(startMinute, endMinute))
                adapter.refreshRow(appInfo.packageName)
            }
            .setNeutralButton(R.string.clear_schedule) { _, _ ->
                BlockRepository.setSchedule(this, appInfo.packageName, null)
                adapter.refreshRow(appInfo.packageName)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
