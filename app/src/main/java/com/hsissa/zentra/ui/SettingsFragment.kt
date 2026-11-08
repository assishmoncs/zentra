package com.hsissa.zentra.ui

import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.InputType
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.room.withTransaction
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.hsissa.zentra.R
import com.hsissa.zentra.BuildConfig
import com.hsissa.zentra.core.SettingsManager
import com.hsissa.zentra.core.AppLimitManager
import com.hsissa.zentra.data.local.AppDatabase
import com.hsissa.zentra.data.local.AppLimitEntity
import android.widget.EditText
import android.widget.Toast
import com.hsissa.zentra.databinding.DialogAppSearchBinding
import com.hsissa.zentra.databinding.FragmentSettingsBinding
import com.hsissa.zentra.service.AppCategory
import com.hsissa.zentra.service.UsageStatsHelper
import com.hsissa.zentra.service.TodayUsageResult
import com.hsissa.zentra.util.TimeFormatter
import com.hsissa.zentra.widget.ZentraWidgetProvider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var settingsManager: SettingsManager

    private lateinit var adapter: AppCategoryAdapter
    private var allApps: List<AppItem> = emptyList()
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted && isAdded) Toast.makeText(requireContext(), R.string.notification_explanation, Toast.LENGTH_LONG).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!::settingsManager.isInitialized) {
            settingsManager = SettingsManager.getInstance(requireContext())
        }

        setupGoalSlider()
        setupQuietHours()
        setupClickListeners()
        setupRecyclerView()

        if (settingsManager.usingFallbackPreferences) {
            Toast.makeText(requireContext(), R.string.settings_recovery_warning, Toast.LENGTH_LONG).show()
        }

        loadApps()
    }

    private fun setupClickListeners() {
        binding.btnManageLimits.setOnClickListener { showAppLimits() }
        binding.btnFocusHistory.setOnClickListener { showFocusHistory() }
        binding.btnShareDiagnostics.setOnClickListener { shareDiagnostics() }
        binding.btnDeleteHistory.setOnClickListener { confirmDeleteHistory() }
        binding.btnDefaultThreshold.setOnClickListener { chooseDefaultThreshold() }
        binding.switchDefaultReminders.isChecked = settingsManager.areDefaultRemindersEnabled()
        binding.switchDefaultReminders.setOnCheckedChangeListener { _, checked ->
            settingsManager.setDefaultRemindersEnabled(checked)
        }
        binding.btnNotificationSettings.setOnClickListener { requestNotifications() }
        binding.btnHelp.setOnClickListener {
            showHelpDialog()
        }

        binding.btnShowMore.setOnClickListener {
            showSearchDialog()
        }

        binding.btnStartTime.setOnClickListener {
            showTimePicker(true)
        }

        binding.btnEndTime.setOnClickListener {
            showTimePicker(false)
        }
    }

    private fun shareDiagnostics() {
        val context = requireContext()
        val report = "Zentra ${BuildConfig.VERSION_NAME}\nAndroid API ${Build.VERSION.SDK_INT}\n" +
            "Usage access: ${UsageStatsHelper.hasUsagePermission(context)}\n" +
            "Notifications enabled: ${NotificationManagerCompat.from(context).areNotificationsEnabled()}\n" +
            "Secure settings available: ${!settingsManager.usingFallbackPreferences}"
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, report)
        startActivity(Intent.createChooser(intent, getString(R.string.share_diagnostics_title)))
    }

    private fun chooseDefaultThreshold() {
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = getString(R.string.limit_minutes_hint)
            setText(settingsManager.getDefaultReminderMinutes().toString())
        }
        val dialog = MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.default_threshold)
            .setView(input).setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null).create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = input.text.toString().toIntOrNull()
                if (value == null || value !in 1..1440) input.error = getString(R.string.limit_invalid)
                else { settingsManager.setDefaultReminderMinutes(value); dialog.dismiss() }
            }
        }
        dialog.show()
    }

    private fun showFocusHistory() {
        val database = AppDatabase.getDatabase(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            val (sessions, streak) = withContext(Dispatchers.IO) {
                database.usageDao().getAllFocusSessions() to database.usageDao().getFocusStreak()
            }
            val today = LocalDate.now()
            val lastDay = streak?.lastGoalMetTimestamp?.takeIf { it > 0 }
                ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
            val current = if (lastDay == today || lastDay == today.minusDays(1)) streak?.currentStreak ?: 0 else 0
            val header = getString(R.string.focus_history_streak, current, streak?.longestStreak ?: 0, streak?.totalFocusMinutes ?: 0L)
            val entries = sessions.take(30).joinToString("\n") {
                "${TimeFormatter.formatDate(it.timestamp)} · ${it.durationMinutes} min · ${it.sessionType}"
            }.ifEmpty { getString(R.string.focus_history_empty) }
            MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.focus_history).setMessage(header + entries)
                .setPositiveButton(R.string.close, null).show()
        }
    }

    private fun confirmDeleteHistory() {
        MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.delete_history)
            .setMessage(R.string.delete_history_confirm).setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.delete_history) { _, _ ->
                val database = AppDatabase.getDatabase(requireContext())
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        database.withTransaction {
                            database.usageDao().deleteUsageHistory()
                            database.usageDao().deleteFocusSessions()
                            database.usageDao().deleteFocusStreaks()
                        }
                    }
                    settingsManager.clearLastFocusScore()
                    ZentraWidgetProvider.notifyWidgetDataChanged(requireContext())
                    Toast.makeText(requireContext(), R.string.history_deleted, Toast.LENGTH_LONG).show()
                }
            }.show()
    }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            if (settingsManager.notificationPermissionRequested() && !shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
                startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName))
                return
            }
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.notification_explanation)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    settingsManager.markNotificationPermissionRequested()
                    notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } else {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName))
        }
    }

    private fun showAppLimits() {
        if (allApps.isEmpty()) {
            Toast.makeText(requireContext(), R.string.no_apps_for_limits, Toast.LENGTH_SHORT).show()
            return
        }
        val manager = AppLimitManager(AppDatabase.getDatabase(requireContext()).usageDao())
        val context = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val (limits, usage) = withContext(Dispatchers.IO) {
                val limits = manager.getAllLimits().associateBy { it.packageName }
                val result = UsageStatsHelper.getTodaySummaryResult(context)
                val usage = (result as? TodayUsageResult.Success)?.summary?.fullUsageList?.associateBy { it.packageName }
                limits to usage
            }
            val labels = allApps.map { app ->
                val limit = limits[app.packageName]
                val used = usage?.get(app.packageName)?.let { " · ${getString(R.string.app_usage_current, it.formattedTime)}" }.orEmpty()
                if (limit == null) app.appName + used else "${app.appName} — ${limit.limitMinutes} min/day${if (limit.isEnabled) "" else " (${getString(R.string.reminder_paused)})"}$used"
            }.toTypedArray()
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.manage_limits)
                .setItems(labels) { _, index ->
                    val app = allApps[index]
                    val limit = limits[app.packageName]
                    if (limit == null) editAppLimit(app, null, manager)
                    else manageExistingLimit(app, limit, manager)
                }
                .setNegativeButton(R.string.close, null)
                .show()
        }
    }

    private fun manageExistingLimit(app: AppItem, limit: AppLimitEntity, manager: AppLimitManager) {
        val choices = arrayOf(getString(R.string.edit_reminder), getString(if (limit.isEnabled) R.string.pause_reminder else R.string.resume_reminder), getString(R.string.limit_removed))
        MaterialAlertDialogBuilder(requireContext()).setTitle(app.appName).setItems(choices) { _, which ->
            when (which) {
                0 -> editAppLimit(app, limit, manager)
                1 -> viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) { manager.setEnabled(limit, !limit.isEnabled) }
                2 -> viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) { manager.removeLimit(app.packageName) }
            }
        }.setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun editAppLimit(app: AppItem, currentLimit: AppLimitEntity?, manager: AppLimitManager) {
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = getString(R.string.limit_minutes_hint)
            currentLimit?.let { setText(it.limitMinutes.toString()) }
        }
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(app.appName)
            .setView(input)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
        if (currentLimit != null) builder.setNeutralButton(R.string.limit_removed) { _, _ ->
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) { manager.removeLimit(app.packageName) }
        }
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val minutes = input.text.toString().toIntOrNull()
                if (minutes == null || minutes !in 1..1440) {
                    input.error = getString(R.string.limit_invalid)
                } else {
                    viewLifecycleOwner.lifecycleScope.launch {
                        withContext(Dispatchers.IO) { manager.setLimit(app.packageName, minutes, currentLimit?.isEnabled ?: true) }
                        if (!NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) requestNotifications()
                    }
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun setupRecyclerView() {
        adapter = AppCategoryAdapter(emptyList()) { app ->
            showCategoryPicker(app, adapter)
        }
        binding.rvAppCategories.adapter = adapter
    }

    private fun showHelpDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_help_title)
            .setMessage(Html.fromHtml(getString(R.string.settings_help_content), Html.FROM_HTML_MODE_COMPACT))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showSearchDialog() {
        val dialogBinding = DialogAppSearchBinding.inflate(layoutInflater)

        lateinit var searchAdapter: AppCategoryAdapter
        searchAdapter = AppCategoryAdapter(allApps) { app ->
            showCategoryPicker(app, searchAdapter)
        }
        dialogBinding.rvSearchApps.adapter = searchAdapter

        dialogBinding.etSearch.addTextChangedListener { text ->
            val query = text.toString().lowercase()
            val filtered = allApps.filter { it.appName.lowercase().contains(query) }
            searchAdapter.updateData(filtered)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.close, null)
            .show()
    }

    private fun setupGoalSlider() {
        val currentGoal = settingsManager.getDailyGoal()
        binding.sliderGoal.value = currentGoal.toFloat()
        binding.tvGoalValue.text = currentGoal.toString()

        binding.sliderGoal.addOnChangeListener { _, value, _ ->
            val goal = value.toInt()
            settingsManager.setDailyGoal(goal)
            binding.tvGoalValue.text = goal.toString()
        }
    }

    private fun setupQuietHours() {
        binding.switchQuietHours.isChecked = settingsManager.isQuietHoursEnabled()
        updateQuietHoursUi(binding.switchQuietHours.isChecked)

        val (startH, startM) = settingsManager.getQuietHoursStart()
        binding.tvStartTime.text = String.format(Locale.getDefault(), "%02d:%02d", startH, startM)

        val (endH, endM) = settingsManager.getQuietHoursEnd()
        binding.tvEndTime.text = String.format(Locale.getDefault(), "%02d:%02d", endH, endM)

        binding.switchQuietHours.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setQuietHoursEnabled(isChecked)
            updateQuietHoursUi(isChecked)
        }
    }

    private fun updateQuietHoursUi(enabled: Boolean) {
        binding.tvQuietHoursDescription.setText(
            if (enabled) R.string.settings_quiet_enabled else R.string.settings_quiet_disabled
        )
        binding.layoutTimePickers.alpha = if (enabled) 1.0f else 0.5f
        binding.btnStartTime.isEnabled = enabled
        binding.btnEndTime.isEnabled = enabled
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
                binding.tvStartTime.text = String.format(Locale.getDefault(), "%02d:%02d", picker.hour, picker.minute)
            } else {
                settingsManager.setQuietHoursEnd(picker.hour, picker.minute)
                binding.tvEndTime.text = String.format(Locale.getDefault(), "%02d:%02d", picker.hour, picker.minute)
            }
        }

        picker.show(childFragmentManager, "time_picker")
    }

    private fun loadApps() {
        val context = requireContext().applicationContext
        val pm = context.packageManager
        viewLifecycleOwner.lifecycleScope.launch {
            val appItems = withContext(Dispatchers.IO) {
                val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                pm.queryIntentActivities(launcherIntent, 0)
                    .distinctBy { it.activityInfo.packageName }
                    .filter { it.activityInfo.packageName != context.packageName }
                    .map { info ->
                        val packageName = info.activityInfo.packageName
                        val appName = info.loadLabel(pm).toString()
                        val category = UsageStatsHelper.getCategory(context, packageName)
                        // Avoid eagerly decoding Drawable bitmaps for all installed apps into memory;
                        // AppCategoryAdapter loads and caches icons lazily on bind.
                        AppItem(packageName = packageName, appName = appName, icon = null, category = category)
                    }.sortedBy { it.appName }
            }
            allApps = appItems
            adapter.updateData(appItems.take(5))
        }
    }

    private fun showCategoryPicker(app: AppItem, targetAdapter: AppCategoryAdapter) {
        val categories = AppCategory.entries.filter { it != AppCategory.SYSTEM }
        val names = categories.map { it.displayName }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.settings_select_category_title, app.appName))
            .setItems(names) { _, which ->
                val selectedCategory = categories[which]
                settingsManager.setAppCategory(app.packageName, selectedCategory)
                app.category = selectedCategory
                adapter.notifyDataSetChanged()
                if (targetAdapter !== adapter) {
                    targetAdapter.notifyDataSetChanged()
                }
            }
            .setNeutralButton(R.string.reset_category) { _, _ ->
                settingsManager.resetAppCategory(app.packageName)
                app.category = UsageStatsHelper.getCategory(requireContext(), app.packageName)
                adapter.notifyDataSetChanged()
                if (targetAdapter !== adapter) targetAdapter.notifyDataSetChanged()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
