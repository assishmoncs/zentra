package com.hsissa.zentra.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.hsissa.zentra.R
import com.hsissa.zentra.core.ScoreManager
import com.hsissa.zentra.service.TodayUsageResult
import com.hsissa.zentra.service.UsageStatsHelper
import com.hsissa.zentra.util.TimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InsightsFragment : Fragment() {

    private lateinit var trendChartView: TrendChartView
    private lateinit var tvWeeklyAvg: TextView
    private lateinit var tvWeeklyTotal: TextView
    private lateinit var tvDetailDate: TextView
    private lateinit var tvDetailStats: TextView
    private lateinit var rvDailyBreakdown: RecyclerView
    private lateinit var adapter: AppUsageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_insights, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        trendChartView = view.findViewById(R.id.trendChartView)
        tvWeeklyAvg = view.findViewById(R.id.tvWeeklyAvg)
        tvWeeklyTotal = view.findViewById(R.id.tvWeeklyTotal)
        tvDetailDate = view.findViewById(R.id.tvDetailDate)
        tvDetailStats = view.findViewById(R.id.tvDetailStats)
        rvDailyBreakdown = view.findViewById(R.id.rvDailyBreakdown)

        adapter = AppUsageAdapter(emptyList())
        rvDailyBreakdown.adapter = adapter

        trendChartView.onItemSelected = { summary ->
            summary?.let {
                val dateStr = TimeFormatter.formatDate(it.dayTimestamp)
                tvDetailDate.text = getString(R.string.insight_detail_date, dateStr)
                tvDetailStats.text = getString(
                    R.string.insight_detail_stats,
                    TimeFormatter.formatMillis(it.totalScreenTimeMillis),
                    ScoreManager.computeScore(it.weightedScreenTimeMillis)
                )
                tvDetailStats.visibility = View.VISIBLE
                tvDetailDate.visibility = View.VISIBLE
            }
        }

        loadInsights()
    }

    private fun loadInsights() {
        viewLifecycleOwner.lifecycleScope.launch {
            val context = context ?: return@launch
            
            val (weeklyTrend, todayResult) = withContext(Dispatchers.IO) {
                val trend = UsageStatsHelper.getWeeklyTrend(context)
                val today = UsageStatsHelper.getTodaySummaryResult(context)
                trend to today
            }

            if (!isAdded) return@launch

            if (weeklyTrend.isNotEmpty()) {
                trendChartView.setData(weeklyTrend)
                
                val totalTime = weeklyTrend.sumOf { it.totalScreenTimeMillis }
                val avgScore = if (weeklyTrend.isNotEmpty()) {
                    ScoreManager.computeScore(weeklyTrend.sumOf { it.weightedScreenTimeMillis } / weeklyTrend.size)
                } else 0

                tvWeeklyAvg.text = getString(R.string.weekly_avg_score, avgScore)
                tvWeeklyTotal.text = getString(
                    R.string.weekly_total_time,
                    TimeFormatter.formatMillis(totalTime)
                )
            }

            if (todayResult is TodayUsageResult.Success) {
                adapter.updateData(todayResult.summary.fullUsageList)
            } else if (todayResult is TodayUsageResult.Empty) {
                adapter.updateData(todayResult.summary.fullUsageList)
            }
        }
    }
}
