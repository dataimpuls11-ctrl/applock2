package com.appblocker.timelock

import android.content.Context
import org.json.JSONObject

/**
 * Holds per-package blocking rules:
 *  - alwaysBlocked: packages that are blocked at all times.
 *  - schedules: packages that are blocked only during a daily time window
 *               (startMinute..endMinute, in minutes since midnight, device local time).
 *
 * Data is stored as a small JSON blob inside SharedPreferences so there are
 * no extra library dependencies.
 */
data class Schedule(val startMinute: Int, val endMinute: Int) {
    /** Handles overnight ranges too, e.g. 22:00 -> 06:00. */
    fun contains(nowMinute: Int): Boolean {
        return if (startMinute <= endMinute) {
            nowMinute in startMinute..endMinute
        } else {
            nowMinute >= startMinute || nowMinute <= endMinute
        }
    }

    fun format(): String {
        fun fmt(m: Int): String = String.format("%02d:%02d", m / 60, m % 60)
        return "${fmt(startMinute)} - ${fmt(endMinute)}"
    }
}

object BlockRepository {

    private const val PREFS = "app_blocker_prefs"
    private const val KEY_ALWAYS = "always_blocked"
    private const val KEY_SCHEDULES = "schedules"
    private const val KEY_PREVENT_REINSTALL = "prevent_reinstall"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getAlwaysBlocked(context: Context): MutableSet<String> {
        val raw = prefs(context).getString(KEY_ALWAYS, "[]") ?: "[]"
        val arr = org.json.JSONArray(raw)
        val set = mutableSetOf<String>()
        for (i in 0 until arr.length()) set.add(arr.getString(i))
        return set
    }

    fun setAlwaysBlocked(context: Context, pkg: String, blocked: Boolean) {
        val set = getAlwaysBlocked(context)
        if (blocked) set.add(pkg) else set.remove(pkg)
        val arr = org.json.JSONArray()
        set.forEach { arr.put(it) }
        prefs(context).edit().putString(KEY_ALWAYS, arr.toString()).apply()
    }

    fun getSchedules(context: Context): MutableMap<String, Schedule> {
        val raw = prefs(context).getString(KEY_SCHEDULES, "{}") ?: "{}"
        val obj = JSONObject(raw)
        val map = mutableMapOf<String, Schedule>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val pkg = keys.next()
            val entry = obj.getJSONObject(pkg)
            map[pkg] = Schedule(entry.getInt("start"), entry.getInt("end"))
        }
        return map
    }

    fun setSchedule(context: Context, pkg: String, schedule: Schedule?) {
        val map = getSchedules(context)
        if (schedule == null) map.remove(pkg) else map[pkg] = schedule
        val obj = JSONObject()
        map.forEach { (p, s) ->
            val entry = JSONObject()
            entry.put("start", s.startMinute)
            entry.put("end", s.endMinute)
            obj.put(p, entry)
        }
        prefs(context).edit().putString(KEY_SCHEDULES, obj.toString()).apply()
    }

    // ── Prevent Re-installation ──────────────────────────────────────────────
    /** Returns the set of packages for which re-installation must be prevented. */
    fun getPreventReinstall(context: Context): MutableSet<String> {
        val raw = prefs(context).getString(KEY_PREVENT_REINSTALL, "[]") ?: "[]"
        val arr = org.json.JSONArray(raw)
        val set = mutableSetOf<String>()
        for (i in 0 until arr.length()) set.add(arr.getString(i))
        return set
    }

    fun setPreventReinstall(context: Context, pkg: String, prevent: Boolean) {
        val set = getPreventReinstall(context)
        if (prevent) set.add(pkg) else set.remove(pkg)
        val arr = org.json.JSONArray()
        set.forEach { arr.put(it) }
        prefs(context).edit().putString(KEY_PREVENT_REINSTALL, arr.toString()).apply()
    }

    fun shouldPreventReinstall(context: Context, pkg: String): Boolean =
        getPreventReinstall(context).contains(pkg)

    // ────────────────────────────────────────────────────────────────────────
    fun isBlockedNow(context: Context, pkg: String): Boolean {
        if (getAlwaysBlocked(context).contains(pkg)) return true
        val schedule = getSchedules(context)[pkg] ?: return false
        val cal = java.util.Calendar.getInstance()
        val nowMinute = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        return schedule.contains(nowMinute)
    }
}
