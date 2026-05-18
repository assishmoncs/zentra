package com.hsissa.zentra.ui

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.hsissa.zentra.R
import com.hsissa.zentra.core.ScoreManager
import com.hsissa.zentra.service.DailyUsageSummary
import com.hsissa.zentra.service.TodayUsageResult
import com.hsissa.zentra.service.UsageStatsHelper
import com.hsissa.zentra.util.TimeFormatter
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class DashboardFragment : Fragment() {

    // --- Views ---
    private lateinit var layoutPermission: LinearLayout
    private lateinit var layoutContent: LinearLayout
    private lateinit var btnGrantPermission: Button
    private lateinit var btnRetry: Button
    private lateinit var tvScore: TextView
    private lateinit var tvFeedback: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var tvApp1: TextView
    private lateinit var tvApp2: TextView
    private lateinit var tvApp3: TextView
    private lateinit var tvNoApps: TextView
    private lateinit var tvState: TextView
    private lateinit var tvGoalStatus: TextView
    private lateinit var tvWeeklyAvgScore: TextView
    private lateinit var tvWeeklyTotalTime: TextView

    // --- Focus Session ---
    private lateinit var tvFocusTimer: TextView
    private lateinit var btnFocusToggle: MaterialButton
    private var countDownTimer: CountDownTimer? = null
    private var isFocusRunning = false

    private val usageExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var loadTask: Future<*>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)

        btnGrantPermission.setOnClickListener {
            openUsageAccessSettings()
        }

        btnRetry.setOnClickListener {
            loadAndDisplay()
        }

        btnFocusToggle.setOnClickListener {
            toggleFocusSession()
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
        loadTask?.cancel(true)
        usageExecutor.shutdownNow()
        countDownTimer?.cancel()
        super.onDestroyView()
    }

    private fun bindViews(view: View) {
        layoutPermission = view.findViewById(R.id.layoutPermission)
        layoutContent = view.findViewById(R.id.layoutContent)
        btnGrantPermission = view.findViewById(R.id.btnGrantPermission)
        btnRetry = view.findViewById(R.id.btnRetry)
        tvScore = view.findViewById(R.id.tvScore)
        tvFeedback = view.findViewById(R.id.tvFeedback)
        tvTotalTime = view.findViewById(R.id.tvTotalTime)
        tvApp1 = view.findViewById(R.id.tvApp1)
        tvApp2 = view.findViewById(R.id.tvApp2)
        tvApp3 = view.findViewById(R.id.tvApp3)
        tvNoApps = view.findViewById(R.id.tvNoApps)
        tvState = view.findViewById(R.id.tvState)
        tvGoalStatus = view.findViewById(R.id.tvGoalStatus)
        tvWeeklyAvgScore = view.findViewById(R.id.tvWeeklyAvgScore)
        tvWeeklyTotalTime = view.findViewById(R.id.tvWeeklyTotalTime)

        tvFocusTimer = view.findViewById(R.id.tvFocusTimer)
        btnFocusToggle = view.findViewById(R.id.btnFocusToggle)
    }

    private fun showPermissionRequest() {
        layoutPermission.visibility = View.VISIBLE
        layoutContent.visibility = View.GONE
    }

    private fun openUsageAccessSettings() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    private fun showContent() {
        layoutPermission.visibility = View.GONE
        layoutContent.visibility = View.VISIBLE
        loadAndDisplay()
    }

    private fun loadAndDisplay() {
        showLoadingState()

        loadTask?.cancel(true)
        loadTask = usageExecutor.submit {
            if (Thread.currentThread().isInterrupted) return@submit
            val context = context ?: return@submit
            val result = UsageStatsHelper.getTodaySummaryResult(context)
            val weeklyResult = UsageStatsHelper.getSummaryResultForDays(context, 7)

            if (Thread.currentThread().isInterrupted) return@submit
            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread
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

                if (weeklyResult is TodayUsageResult.Success) {
                    renderWeeklyTrends(weeklyResult.summary)
                }
            }
        }
    }

    private fun renderSummary(usageSummary: DailyUsageSummary) {
        val score = ScoreManager.computeScore(usageSummary.weightedScreenTimeMillis)
        val feedbackResId = ScoreManager.getFeedbackResId(score)

        tvScore.text = score.toString()
        tvFeedback.setText(feedbackResId)
        tvScore.setTextColor(scoreColor(score))

        tvTotalTime.text = getString(
            R.string.screen_time_today,
            TimeFormatter.formatMillis(usageSummary.totalScreenTimeMillis),
        )

        renderTopApps(usageSummary)
        renderGoalStatus(score)
    }

    private fun renderGoalStatus(currentScore: Int) {
        if (currentScore >= DAILY_GOAL_SCORE) {
            tvGoalStatus.text = getString(R.string.goal_reached)
            tvGoalStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.score_high))
        } else {
            val remaining = DAILY_GOAL_SCORE - currentScore
            tvGoalStatus.text = getString(R.string.goal_remaining, remaining)
            tvGoalStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }
    }

    private fun renderWeeklyTrends(weeklySummary: DailyUsageSummary) {
        val avgScore = ScoreManager.computeScore(weeklySummary.weightedScreenTimeMillis / 7)
        tvWeeklyAvgScore.text = getString(R.string.weekly_avg_score, avgScore)
        tvWeeklyTotalTime.text = getString(
            R.string.weekly_total_time,
            TimeFormatter.formatMillis(weeklySummary.totalScreenTimeMillis)
        )
    }

    private fun renderTopApps(summary: DailyUsageSummary) {
        val apps = summary.topApps
        val appViews = listOf(tvApp1, tvApp2, tvApp3)

        if (apps.isEmpty()) {
            tvNoApps.visibility = View.VISIBLE
            appViews.forEach { it.visibility = View.GONE }
            return
        }

        tvNoApps.visibility = View.GONE
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
        tvState.visibility = View.VISIBLE
        tvState.text = getString(R.string.usage_state_loading)
        btnRetry.visibility = View.GONE
    }

    private fun showErrorState(message: String, showRetry: Boolean) {
        tvState.visibility = View.VISIBLE
        tvState.text = message
        btnRetry.visibility = if (showRetry) View.VISIBLE else View.GONE
    }

    private fun hideState() {
        tvState.visibility = View.GONE
        btnRetry.visibility = View.GONE
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
        btnFocusToggle.text = getString(R.string.focus_session_stop)

        countDownTimer = object : CountDownTimer(FOCUS_DURATION_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                tvFocusTimer.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                isFocusRunning = false
                tvFocusTimer.text = getString(R.string.focus_session_finished)
                btnFocusToggle.text = getString(R.string.focus_session_start)
            }
        }.start()
    }

    private fun stopFocusSession() {
        countDownTimer?.cancel()
        isFocusRunning = false
        btnFocusToggle.text = getString(R.string.focus_session_start)
        tvFocusTimer.text = getString(R.string.focus_session_timer_placeholder)
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
        private const val DAILY_GOAL_SCORE = 80
    }
}
