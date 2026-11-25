package com.appblocker.timelock

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial

class AppListAdapter(
    private val context: android.content.Context,
    private val apps: List<AppInfo>,
    private val onScheduleClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.appIcon)
        val name: TextView = view.findViewById(R.id.appName)
        val switchAlways: SwitchMaterial = view.findViewById(R.id.switchAlwaysBlock)
        val scheduleText: TextView = view.findViewById(R.id.scheduleText)
        val btnSchedule: Button = view.findViewById(R.id.btnSetSchedule)
        val switchPreventReinstall: SwitchMaterial = view.findViewById(R.id.switchPreventReinstall)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.icon.setImageDrawable(app.icon)
        holder.name.text = app.label

        // Avoid listener firing while we set initial state from storage.
        holder.switchAlways.setOnCheckedChangeListener(null)
        holder.switchAlways.isChecked = BlockRepository.getAlwaysBlocked(context).contains(app.packageName)
        holder.switchAlways.setOnCheckedChangeListener { _, isChecked ->
            BlockRepository.setAlwaysBlocked(context, app.packageName, isChecked)
        }

        val schedule = BlockRepository.getSchedules(context)[app.packageName]
        holder.scheduleText.text = schedule?.format() ?: context.getString(R.string.no_schedule)

        holder.btnSchedule.setOnClickListener { onScheduleClick(app) }

        // ── Prevent Re-installation toggle ───────────────────────────────────
        holder.switchPreventReinstall.setOnCheckedChangeListener(null)
        holder.switchPreventReinstall.isChecked =
            BlockRepository.shouldPreventReinstall(context, app.packageName)
        holder.switchPreventReinstall.setOnCheckedChangeListener { _, isChecked ->
            BlockRepository.setPreventReinstall(context, app.packageName, isChecked)
        }
    }

    override fun getItemCount(): Int = apps.size

    fun refreshRow(packageName: String) {
        val index = apps.indexOfFirst { it.packageName == packageName }
        if (index >= 0) notifyItemChanged(index)
    }
}
