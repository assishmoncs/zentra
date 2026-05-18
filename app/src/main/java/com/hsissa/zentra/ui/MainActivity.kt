package com.hsissa.zentra.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.hsissa.zentra.R
import com.hsissa.zentra.core.ScoreManager
import com.hsissa.zentra.service.DailyUsageSummary
import com.hsissa.zentra.service.UsageStatsHelper
import com.hsissa.zentra.util.TimeFormatter

/**
 * Main (and only) screen of Zentra.
 * Displays the life score, screen time, top apps, and feedback.
 */
class MainActivity : AppCompatActivity() {

    // --- Views ---
    private lateinit var layoutPermission: LinearLayout
    private lateinit var layoutContent: LinearLayout
    private lateinit var btnGrantPermission: Button
    private lateinit var tvScore: TextView
    private lateinit var tvFeedback: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var tvApp1: TextView
    private lateinit var tvApp2: TextView
    private lateinit var tvApp3: TextView
    private lateinit var tvNoApps: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()

        btnGrantPermission.setOnClickListener {
            openUsageAccessSettings()
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check permission every time the screen comes into focus
        // (user may have just returned from Settings)
        if (UsageStatsHelper.hasUsagePermission(this)) {
            showContent()
        } else {
            showPermissionRequest()
        }
    }

    // -------------------------------------------------------------------------
    // View binding
    // -------------------------------------------------------------------------

    private fun bindViews() {
        layoutPermission  = findViewById(R.id.layoutPermission)
        layoutContent     = findViewById(R.id.layoutContent)
        btnGrantPermission = findViewById(R.id.btnGrantPermission)
        tvScore           = findViewById(R.id.tvScore)
        tvFeedback        = findViewById(R.id.tvFeedback)
        tvTotalTime       = findViewById(R.id.tvTotalTime)
        tvApp1            = findViewById(R.id.tvApp1)
        tvApp2            = findViewById(R.id.tvApp2)
        tvApp3            = findViewById(R.id.tvApp3)
        tvNoApps          = findViewById(R.id.tvNoApps)
    }

    // -------------------------------------------------------------------------
    // Permission UI
    // -------------------------------------------------------------------------

    private fun showPermissionRequest() {
        layoutPermission.visibility = View.VISIBLE
        layoutContent.visibility   = View.GONE
    }

    private fun openUsageAccessSettings() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    // -------------------------------------------------------------------------
    // Content UI
    // -------------------------------------------------------------------------

    private fun showContent() {
        layoutPermission.visibility = View.GONE
        layoutContent.visibility   = View.VISIBLE
        loadAndDisplay()
    }

    private fun loadAndDisplay() {
        val usageSummary = UsageStatsHelper.getTodaySummary(this)
        val score = ScoreManager.computeScore(usageSummary.totalScreenTimeMillis)
        val feedbackResId = ScoreManager.getFeedbackResId(score)

        // Score
        tvScore.text = score.toString()
        tvFeedback.setText(feedbackResId)

        // Apply color tint based on score range
        tvScore.setTextColor(scoreColor(score))

        // Total screen time
        tvTotalTime.text = getString(
            R.string.screen_time_today,
            TimeFormatter.formatMillis(usageSummary.totalScreenTimeMillis),
        )

        // Top apps
        renderTopApps(usageSummary)
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
                view.text       = getString(R.string.app_usage_item, index + 1, app.appName, app.formattedTime)
                view.visibility = View.VISIBLE
            } else {
                view.visibility = View.GONE
            }
        }
    }

    /**
     * Returns a color resource int based on the score value.
     * Green for high, amber for mid, red for low.
     */
    private fun scoreColor(score: Int): Int {
        return when {
            ScoreManager.isHighScore(score) -> ContextCompat.getColor(this, R.color.score_high)
            ScoreManager.isMidScore(score) -> ContextCompat.getColor(this, R.color.score_mid)
            else -> ContextCompat.getColor(this, R.color.score_low)
        }
    }
}
