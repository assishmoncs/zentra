package com.hsissa.zentra.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.Locale
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hsissa.zentra.R
import com.hsissa.zentra.core.SettingsManager
import com.hsissa.zentra.service.AppCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment() {

    private lateinit var rvApps: RecyclerView
    private lateinit var adapter: AppCategoryAdapter
    private lateinit var settingsManager: SettingsManager
    private lateinit var sliderGoal: Slider
    private lateinit var tvGoalValue: TextView
    private lateinit var switchQuietHours: MaterialSwitch
    private lateinit var tvStartTime: TextView
    private lateinit var tvEndTime: TextView
    private lateinit var layoutTimePickers: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvApps = view.findViewById(R.id.rvAppCategories)
        settingsManager = SettingsManager(requireContext())
        sliderGoal = view.findViewById(R.id.sliderGoal)
        tvGoalValue = view.findViewById(R.id.tvGoalValue)
        switchQuietHours = view.findViewById(R.id.switchQuietHours)
        tvStartTime = view.findViewById(R.id.tvStartTime)
        tvEndTime = view.findViewById(R.id.tvEndTime)
        layoutTimePickers = view.findViewById(R.id.layoutTimePickers)

        setupGoalSlider()
        setupQuietHours()

        adapter = AppCategoryAdapter(emptyList()) { app ->
            showCategoryPicker(app)
        }
        rvApps.adapter = adapter

        loadApps()
    }

    private fun setupGoalSlider() {
        val currentGoal = settingsManager.getDailyGoal()
        sliderGoal.value = currentGoal.toFloat()
        tvGoalValue.text = currentGoal.toString()

        sliderGoal.addOnChangeListener { _, value, _ ->
            val goal = value.toInt()
            settingsManager.setDailyGoal(goal)
            tvGoalValue.text = goal.toString()
        }
    }

    private fun setupQuietHours() {
        switchQuietHours.isChecked = settingsManager.isQuietHoursEnabled()
        updateQuietHoursUi(switchQuietHours.isChecked)

        val (startH, startM) = settingsManager.getQuietHoursStart()
        tvStartTime.text = String.format(Locale.getDefault(), "%02d:%02d", startH, startM)

        val (endH, endM) = settingsManager.getQuietHoursEnd()
        tvEndTime.text = String.format(Locale.getDefault(), "%02d:%02d", endH, endM)

        switchQuietHours.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setQuietHoursEnabled(isChecked)
            updateQuietHoursUi(isChecked)
        }

        view?.findViewById<View>(R.id.btnStartTime)?.setOnClickListener {
            showTimePicker(true)
        }

        view?.findViewById<View>(R.id.btnEndTime)?.setOnClickListener {
            showTimePicker(false)
        }
    }

    private fun updateQuietHoursUi(enabled: Boolean) {
        layoutTimePickers.alpha = if (enabled) 1.0f else 0.5f
        view?.findViewById<View>(R.id.btnStartTime)?.isEnabled = enabled
        view?.findViewById<View>(R.id.btnEndTime)?.isEnabled = enabled
    }

    private fun showTimePicker(isStart: Boolean) {
        val (currentH, currentM) = if (isStart) settingsManager.getQuietHoursStart() else settingsManager.getQuietHoursEnd()

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(currentH)
            .setMinute(currentM)
            .setTitleText(if (isStart) "Start Time" else "End Time")
            .build()

        picker.addOnPositiveButtonClickListener {
            if (isStart) {
                settingsManager.setQuietHoursStart(picker.hour, picker.minute)
                tvStartTime.text = String.format(Locale.getDefault(), "%02d:%02d", picker.hour, picker.minute)
            } else {
                settingsManager.setQuietHoursEnd(picker.hour, picker.minute)
                tvEndTime.text = String.format(Locale.getDefault(), "%02d:%02d", picker.hour, picker.minute)
            }
        }

        picker.show(childFragmentManager, "time_picker")
    }

    private fun loadApps() {
        val pm = requireContext().packageManager
        viewLifecycleOwner.lifecycleScope.launch {
            val appItems = withContext(Dispatchers.IO) {
                val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                installedApps
                    .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 } // Filter out system apps
                    .map { appInfo ->
                        val packageName = appInfo.packageName
                        val appName = pm.getApplicationLabel(appInfo).toString()
                        val icon = pm.getApplicationIcon(appInfo)
                        val category = settingsManager.getAppCategory(packageName) ?: AppCategory.NEUTRAL
                        AppItem(packageName, appName, icon, category)
                    }.sortedBy { it.appName }
            }
            adapter.updateData(appItems)
        }
    }

    private fun showCategoryPicker(app: AppItem) {
        val categories = AppCategory.values().filter { it != AppCategory.SYSTEM }
        val names = categories.map { it.displayName }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.settings_select_category_title, app.appName))
            .setItems(names) { _, which ->
                val selectedCategory = categories[which]
                settingsManager.setAppCategory(app.packageName, selectedCategory)
                app.category = selectedCategory
                adapter.notifyDataSetChanged()
            }
            .show()
    }
}

