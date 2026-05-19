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

        adapter = AppCategoryAdapter(emptyList()) { app ->
            showCategoryPicker(app)
        }
        rvApps.adapter = adapter

        loadApps()
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
        val categories = AppCategory.values()
        val names = categories.map { it.name }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Category for ${app.appName}")
            .setItems(names) { _, which ->
                val selectedCategory = categories[which]
                settingsManager.setAppCategory(app.packageName, selectedCategory)
                app.category = selectedCategory
                adapter.notifyDataSetChanged()
            }
            .show()
    }
}

