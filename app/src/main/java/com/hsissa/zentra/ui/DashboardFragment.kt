package com.hsissa.zentra.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.text.InputType
import android.widget.EditText
import com.hsissa.zentra.R
import com.hsissa.zentra.core.ScoreManager
import com.hsissa.zentra.core.SettingsManager
import com.hsissa.zentra.databinding.FragmentDashboardBinding
import com.hsissa.zentra.service.DailyUsageSummary
import com.hsissa.zentra.service.FocusSessionService
import com.hsissa.zentra.service.TodayUsageResult
import com.hsissa.zentra.service.UsageStatsHelper
import com.hsissa.zentra.ui.dashboard.DashboardViewModel
import com.hsissa.zentra.util.TimeFormatter
import com.hsissa.zentra.widget.ZentraWidgetProvider
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()

    private var focusService: FocusSessionService? = null
    private var isServiceBound = false
    private var pendingFocusSession: Pair<Int, Boolean>? = null
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        pendingFocusSession?.let { (minutes, isBreak) -> launchFocusSession(minutes, isBreak) }
        pendingFocusSession = null
    }

    @Inject
    lateinit var settingsManager: SettingsManager

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? FocusSessionService.LocalBinder ?: return
            focusService = binder.getService().also { activeService ->
                isServiceBound = true
                syncFocusUiWithService(activeService)
                activeService.onTickListener = { millisUntilFinished ->
                    _binding?.tvFocusTimer?.text = formatTimer(millisUntilFinished)
                }
                activeService.onFinishListener = {
                    _binding?.let { b ->
                        b.tvFocusTimer.text = getString(R.string.focus_session_finished)
                        b.btnFocusToggle.text = getString(R.string.focus_session_start)
                        if (!activeService.isBreak && isAdded) {
                            MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.break_offer)
                                .setPositiveButton(R.string.break_start) { _, _ -> startFocusSession(5, true) }
                                .setNegativeButton(R.string.close, null).show()
                        }
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            focusService = null
            isServiceBound = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!::settingsManager.isInitialized) {
            settingsManager = SettingsManager.getInstance(requireContext())
        }

        setupClickListeners()
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(requireContext(), FocusSessionService::class.java)
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        focusService?.onTickListener = null
        focusService?.onFinishListener = null
        if (isServiceBound) {
            requireContext().unbindService(serviceConnection)
            isServiceBound = false
        }
        super.onStop()
    }

    private fun setupClickListeners() {
        binding.btnGrantPermission.setOnClickListener {
            openUsageAccessSettings()
        }

        binding.btnRetry.setOnClickListener {
            viewModel.loadData()
        }

        binding.btnFocusToggle.setOnClickListener {
            toggleFocusSession()
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading == true) showLoadingState()
            else viewModel.todayUsage.value?.let(::handleTodayUsageResult)
        }

        viewModel.todayUsage.observe(viewLifecycleOwner) { result ->
            result?.let(::handleTodayUsageResult)
        }

        viewModel.weeklyTrend.observe(viewLifecycleOwner) { trend ->
            trend?.takeIf { it.isNotEmpty() }?.let { weeklyTrend ->
                renderWeeklyTrends(weeklyTrend)
            }
        }
    }

    private fun handleTodayUsageResult(result: TodayUsageResult) {
        when (result) {
            is TodayUsageResult.Success -> {
                hideState()
                renderSummary(result.summary)
            }
            is TodayUsageResult.Empty -> {
                if (result.isUnexpected) {
                    clearInvalidScore()
                    showErrorState(
                        getString(R.string.usage_state_unexpected_empty),
                        showRetry = true,
                    )
                } else {
                    renderSummary(result.summary)
                    hideState()
                }
            }
            TodayUsageResult.Error -> {
                clearInvalidScore()
                showErrorState(
                    getString(R.string.usage_state_error),
                    showRetry = true,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (UsageStatsHelper.hasUsagePermission(requireContext())) {
            showContent()
        } else {
            showPermissionRequest()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun showPermissionRequest() {
        binding.layoutPermission.visibility = View.VISIBLE
        binding.layoutContent.visibility = View.VISIBLE
        clearInvalidScore()
        binding.tvWeeklyAvgScore.text = getString(R.string.score_unavailable)
        binding.tvWeeklyTotalTime.text = getString(R.string.score_unavailable)
    }

    private fun clearInvalidScore() {
        settingsManager.clearLastFocusScore()
        ZentraWidgetProvider.notifyWidgetDataChanged(requireContext())
        binding.tvScore.text = getString(R.string.score_placeholder)
        binding.tvScore.contentDescription = getString(R.string.usage_state_error)
        binding.tvFeedback.text = getString(R.string.score_unavailable)
        binding.tvGoalStatus.text = getString(R.string.score_unavailable)
        binding.tvTotalTime.text = getString(R.string.score_unavailable)
        binding.tvNoApps.visibility = View.VISIBLE
        listOf(binding.tvApp1, binding.tvApp2, binding.tvApp3).forEach { it.visibility = View.GONE }
    }

    private fun openUsageAccessSettings() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    private fun showContent() {
        binding.layoutPermission.visibility = View.GONE
        binding.layoutContent.visibility = View.VISIBLE
        viewModel.loadData()
    }

    private fun renderSummary(usageSummary: DailyUsageSummary) {
        val score = ScoreManager.computeScore(usageSummary.weightedScreenTimeMillis)
        val feedbackResId = ScoreManager.getFeedbackResId(score)

        settingsManager.setLastFocusScore(score)
        ZentraWidgetProvider.notifyWidgetDataChanged(requireContext())

        binding.tvScore.text = score.toString()
        binding.tvScore.contentDescription = getString(
            R.string.dashboard_score_content_description,
            score
        )
        binding.tvFeedback.setText(feedbackResId)
        binding.tvScore.setTextColor(scoreColor(score))

        binding.tvTotalTime.text = getString(
            R.string.screen_time_today,
            TimeFormatter.formatMillis(usageSummary.totalScreenTimeMillis),
        )

        renderTopApps(usageSummary)
        renderGoalStatus(score)
    }

    private fun renderGoalStatus(currentScore: Int) {
        val dailyGoal = settingsManager.getDailyGoal()
        if (currentScore >= dailyGoal) {
            binding.tvGoalStatus.text = getString(R.string.goal_reached)
            binding.tvGoalStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.score_high))
        } else {
            binding.tvGoalStatus.text = getString(R.string.goal_below_target)
            binding.tvGoalStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }
    }

    private fun renderWeeklyTrends(weeklyTrend: List<DailyUsageSummary>) {
        val dailyGoal = settingsManager.getDailyGoal()
        val available = weeklyTrend.filter { it.isAvailable }
        val avgScore = ScoreManager.averageDailyScore(available)
        if (avgScore == null) {
            binding.tvWeeklyAvgScore.text = getString(R.string.score_unavailable)
            binding.tvWeeklyTotalTime.text = getString(R.string.score_unavailable)
            return
        }
        binding.tvWeeklyAvgScore.text = getString(R.string.weekly_avg_available, avgScore, available.size)

        if (avgScore >= dailyGoal) {
            binding.tvWeeklyAvgScore.setTextColor(ContextCompat.getColor(requireContext(), R.color.score_high))
        } else {
            binding.tvWeeklyAvgScore.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }

        binding.tvWeeklyTotalTime.text = getString(
            R.string.weekly_total_time,
            TimeFormatter.formatMillis(available.sumOf { it.totalScreenTimeMillis })
        )
    }

    private fun renderTopApps(summary: DailyUsageSummary) {
        val apps = summary.topApps
        val appViews = listOf(binding.tvApp1, binding.tvApp2, binding.tvApp3)

        if (apps.isEmpty()) {
            binding.tvNoApps.visibility = View.VISIBLE
            appViews.forEach { it.visibility = View.GONE }
            return
        }

        binding.tvNoApps.visibility = View.GONE
        appViews.forEachIndexed { index, view ->
            if (index < apps.size) {
                val app = apps[index]
                view.text = getString(R.string.app_usage_item, index + 1, app.appName, app.formattedTime)
                view.visibility = View.VISIBLE
            } else {
                view.visibility = View.GONE
            }
        }
    }

    private fun showLoadingState() {
        binding.tvState.visibility = View.VISIBLE
        binding.tvState.text = getString(R.string.usage_state_loading)
        binding.btnRetry.visibility = View.GONE
    }

    private fun showErrorState(message: String, showRetry: Boolean) {
        binding.tvState.visibility = View.VISIBLE
        binding.tvState.text = message
        binding.btnRetry.visibility = if (showRetry) View.VISIBLE else View.GONE
    }

    private fun hideState() {
        binding.tvState.visibility = View.GONE
        binding.btnRetry.visibility = View.GONE
    }

    private fun syncFocusUiWithService(service: FocusSessionService) {
        if (service.isRunning) {
            binding.btnFocusToggle.text = getString(R.string.focus_session_stop)
            binding.tvFocusTimer.text = formatTimer(service.remainingTimeMillis)
        } else {
            binding.btnFocusToggle.text = getString(R.string.focus_session_start)
            binding.tvFocusTimer.text = getString(R.string.focus_session_timer_placeholder)
        }
    }

    private fun toggleFocusSession() {
        val running = focusService?.isRunning == true || FocusSessionService.isSessionActive
        if (running) {
            stopFocusSession()
        } else {
            chooseFocusDuration()
        }
    }

    private fun chooseFocusDuration() {
        val options = arrayOf(getString(R.string.focus_25), getString(R.string.focus_45), getString(R.string.focus_60), getString(R.string.focus_custom))
        MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.focus_choose_duration)
            .setItems(options) { _, which ->
                if (which < 3) startFocusSession(listOf(25, 45, 60)[which]) else {
                    val input = EditText(requireContext()).apply { inputType = InputType.TYPE_CLASS_NUMBER; hint = getString(R.string.limit_minutes_hint) }
                    val dialog = MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.focus_choose_duration)
                        .setView(input).setPositiveButton(android.R.string.ok, null)
                        .setNegativeButton(android.R.string.cancel, null).create()
                    dialog.setOnShowListener {
                        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val minutes = input.text.toString().toIntOrNull()
                            if (minutes == null || minutes !in 1..1440) input.error = getString(R.string.limit_invalid)
                            else { dialog.dismiss(); startFocusSession(minutes) }
                        }
                    }
                    dialog.show()
                }
            }.setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun startFocusSession(minutes: Int, isBreak: Boolean = false) {
        if (Build.VERSION.SDK_INT >= 33 && !settingsManager.notificationPermissionRequested() &&
            ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pendingFocusSession = minutes to isBreak
            MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.notification_explanation)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    settingsManager.markNotificationPermissionRequested()
                    notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                .setNegativeButton(R.string.focus_without_notifications) { _, _ ->
                    pendingFocusSession = null
                    launchFocusSession(minutes, isBreak)
                }
                .setOnCancelListener { pendingFocusSession = null }
                .show()
            return
        }
        launchFocusSession(minutes, isBreak)
    }

    private fun launchFocusSession(minutes: Int, isBreak: Boolean) {
        if (!isAdded || _binding == null) return
        val startIntent = Intent(requireContext(), FocusSessionService::class.java).apply {
            action = FocusSessionService.ACTION_START_SESSION
            putExtra(FocusSessionService.EXTRA_DURATION_MINUTES, minutes)
            putExtra(FocusSessionService.EXTRA_IS_BREAK, isBreak)
        }
        ContextCompat.startForegroundService(requireContext(), startIntent)
        binding.btnFocusToggle.text = getString(R.string.focus_session_stop)
    }

    private fun stopFocusSession() {
        focusService?.stopSession() ?: run {
            val stopIntent = Intent(requireContext(), FocusSessionService::class.java).apply {
                action = FocusSessionService.ACTION_STOP_SESSION
            }
            requireContext().startService(stopIntent)
        }
        binding.btnFocusToggle.text = getString(R.string.focus_session_start)
        binding.tvFocusTimer.text = getString(R.string.focus_session_timer_placeholder)
    }

    private fun formatTimer(millisUntilFinished: Long): String {
        val minutes = (millisUntilFinished / 1000) / 60
        val seconds = (millisUntilFinished / 1000) % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    private fun scoreColor(score: Int): Int {
        return when {
            ScoreManager.isHighScore(score) -> ContextCompat.getColor(requireContext(), R.color.score_high)
            ScoreManager.isMidScore(score) -> ContextCompat.getColor(requireContext(), R.color.score_mid)
            else -> ContextCompat.getColor(requireContext(), R.color.score_low)
        }
    }
}
