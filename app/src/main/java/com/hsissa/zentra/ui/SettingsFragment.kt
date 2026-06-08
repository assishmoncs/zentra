package com.hsissa.zentra.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.hsissa.zentra.R
import com.hsissa.zentra.core.SettingsManager
import com.hsissa.zentra.databinding.DialogAppSearchBinding
import com.hsissa.zentra.databinding.FragmentSettingsBinding
import com.hsissa.zentra.service.AppCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AppCategoryAdapter
    private lateinit var settingsManager: SettingsManager
    private var allApps: List<AppItem> = emptyList()

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
        settingsManager = SettingsManager(requireContext())

        setupGoalSlider()
        setupQuietHours()
        setupClickListeners()
        setupRecyclerView()

        loadApps()
    }

    private fun setupClickListeners() {
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

    private fun setupRecyclerView() {
        adapter = AppCategoryAdapter(emptyList()) { app ->
            showCategoryPicker(app)
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
        
        val searchAdapter = AppCategoryAdapter(allApps) { app ->
            showCategoryPicker(app)
        }
        dialogBinding.rvSearchApps.adapter = searchAdapter

        dialogBinding.etSearch.addTextChangedListener { text ->
            val query = text.toString().lowercase()
            val filtered = allApps.filter { it.appName.lowercase().contains(query) }
            searchAdapter.updateData(filtered)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.retry, null) // Reusing "Retry" as "Close" if needed or use android.R.string.ok
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
        val pm = requireContext().packageManager
        viewLifecycleOwner.lifecycleScope.launch {
            val appItems = withContext(Dispatchers.IO) {
                val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                installedApps
                    .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
                    .map { appInfo ->
                        val packageName = appInfo.packageName
                        val appName = pm.getApplicationLabel(appInfo).toString()
                        val icon = pm.getApplicationIcon(appInfo)
                        val category = settingsManager.getAppCategory(packageName) ?: AppCategory.NEUTRAL
                        AppItem(packageName, appName, icon, category)
                    }.sortedBy { it.appName }
            }
            allApps = appItems
            adapter.updateData(appItems.take(5))
        }
    }

    private fun showCategoryPicker(app: AppItem) {
        val categories = AppCategory.entries.filter { it != AppCategory.SYSTEM }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
