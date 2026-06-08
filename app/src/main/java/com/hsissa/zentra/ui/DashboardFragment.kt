package com.hsissa.zentra.ui

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.hsissa.zentra.R
import com.hsissa.zentra.core.ScoreManager
import com.hsissa.zentra.core.SettingsManager
import com.hsissa.zentra.data.local.AppDatabase
import com.hsissa.zentra.data.repository.UsageRepository
import com.hsissa.zentra.databinding.FragmentDashboardBinding
import com.hsissa.zentra.service.DailyUsageSummary
import com.hsissa.zentra.service.TodayUsageResult
import com.hsissa.zentra.service.UsageStatsHelper
import com.hsissa.zentra.ui.dashboard.DashboardViewModel
import com.hsissa.zentra.util.TimeFormatter
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()

    private var countDownTimer: CountDownTimer? = null
    private var isFocusRunning = false

    private lateinit var settingsManager: SettingsManager

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
        settingsManager = SettingsManager(requireContext())

        setupClickListeners()
        observeViewModel()
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
            if (isLoading) showLoadingState() else hideState()
        }

        viewModel.todayUsage.observe(viewLifecycleOwner) { result ->
            handleTodayUsageResult(result)
        }

        viewModel.weeklyTrend.observe(viewLifecycleOwner) { trend ->
            if (trend.isNotEmpty()) {
                val totalTime = trend.sumOf { it.totalScreenTimeMillis }
                val totalWeighted = trend.sumOf { it.weightedScreenTimeMillis }
                
                val avgSummary = DailyUsageSummary(
                    totalScreenTimeMillis = totalTime,
                    weightedScreenTimeMillis = totalWeighted,
                    topApps = emptyList()
                )
                renderWeeklyTrends(avgSummary)
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
                renderSummary(result.summary)
                if (result.isUnexpected) {
                    showErrorState(
                        getString(R.string.usage_state_unexpected_empty),
                        showRetry = true,
                    )
                } else {
                    hideState()
                }
            }
            TodayUsageResult.Error -> {
                renderSummary(DailyUsageSummary.EMPTY)
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
        countDownTimer?.cancel()
        _binding = null
        super.onDestroyView()
    }

    private fun showPermissionRequest() {
        binding.layoutPermission.visibility = View.VISIBLE
        binding.layoutContent.visibility = View.GONE
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

        binding.tvScore.text = score.toString()
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
            val remaining = dailyGoal - currentScore
            binding.tvGoalStatus.text = getString(R.string.goal_remaining, remaining)
            binding.tvGoalStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }
    }

    private fun renderWeeklyTrends(weeklySummary: DailyUsageSummary) {
        val dailyGoal = settingsManager.getDailyGoal()
        val avgScore = ScoreManager.computeScore(weeklySummary.weightedScreenTimeMillis / 7)
        binding.tvWeeklyAvgScore.text = getString(R.string.weekly_avg_score, avgScore)
        
        if (avgScore >= dailyGoal) {
            binding.tvWeeklyAvgScore.setTextColor(ContextCompat.getColor(requireContext(), R.color.score_high))
        } else {
            binding.tvWeeklyAvgScore.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }

        binding.tvWeeklyTotalTime.text = getString(
            R.string.weekly_total_time,
            TimeFormatter.formatMillis(weeklySummary.totalScreenTimeMillis)
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

    private fun toggleFocusSession() {
        if (isFocusRunning) {
            stopFocusSession()
        } else {
            startFocusSession()
        }
    }

    private fun startFocusSession() {
        isFocusRunning = true
        binding.btnFocusToggle.text = getString(R.string.focus_session_stop)

        countDownTimer = object : CountDownTimer(FOCUS_DURATION_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                binding.tvFocusTimer.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                isFocusRunning = false
                binding.tvFocusTimer.text = getString(R.string.focus_session_finished)
                binding.btnFocusToggle.text = getString(R.string.focus_session_start)
            }
        }.start()
    }

    private fun stopFocusSession() {
        countDownTimer?.cancel()
        isFocusRunning = false
        binding.btnFocusToggle.text = getString(R.string.focus_session_start)
        binding.tvFocusTimer.text = getString(R.string.focus_session_timer_placeholder)
    }

    private fun scoreColor(score: Int): Int {
        return when {
            ScoreManager.isHighScore(score) -> ContextCompat.getColor(requireContext(), R.color.score_high)
            ScoreManager.isMidScore(score) -> ContextCompat.getColor(requireContext(), R.color.score_mid)
            else -> ContextCompat.getColor(requireContext(), R.color.score_low)
        }
    }

    companion object {
        private const val FOCUS_DURATION_MS = 25 * 60 * 1000L
    }
}
