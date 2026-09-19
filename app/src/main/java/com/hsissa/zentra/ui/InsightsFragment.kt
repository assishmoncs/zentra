package com.hsissa.zentra.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.hsissa.zentra.R
import com.hsissa.zentra.core.ScoreManager
import com.hsissa.zentra.databinding.FragmentInsightsBinding
import com.hsissa.zentra.service.TodayUsageResult
import com.hsissa.zentra.ui.insights.InsightsViewModel
import com.hsissa.zentra.util.TimeFormatter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InsightsFragment : Fragment() {

    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InsightsViewModel by viewModels()

    private lateinit var adapter: AppUsageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInsightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupTrendChart()
        observeViewModel()
        viewModel.loadInsights()
    }

    private fun setupRecyclerView() {
        adapter = AppUsageAdapter(emptyList())
        binding.rvDailyBreakdown.adapter = adapter
    }

    private fun setupTrendChart() {
        binding.trendChartView.onItemSelected = { summary ->
            summary?.let {
                val dateStr = TimeFormatter.formatDate(it.dayTimestamp)
                binding.tvDetailDate.text = getString(R.string.insight_detail_date, dateStr)
                binding.tvDetailStats.text = getString(
                    R.string.insight_detail_stats,
                    TimeFormatter.formatMillis(it.totalScreenTimeMillis),
                    ScoreManager.computeScore(it.weightedScreenTimeMillis)
                )
                binding.tvDetailStats.visibility = View.VISIBLE
                binding.tvDetailDate.visibility = View.VISIBLE
            }
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) {
            renderState()
        }

        viewModel.hasLoadError.observe(viewLifecycleOwner) {
            renderState()
        }

        viewModel.weeklyTrend.observe(viewLifecycleOwner) { weeklyTrend ->
            if (weeklyTrend.isNotEmpty()) {
                binding.trendChartView.setData(weeklyTrend)

                val totalTime = weeklyTrend.sumOf { it.totalScreenTimeMillis }
                val avgScore = ScoreManager.computeScore(
                    weeklyTrend.sumOf { it.weightedScreenTimeMillis } / weeklyTrend.size
                )

                binding.tvWeeklyAvg.text = getString(R.string.insights_value_score, avgScore)
                binding.tvWeeklyTotal.text = getString(
                    R.string.insights_value_time,
                    TimeFormatter.formatMillis(totalTime)
                )
            }
            renderState()
        }

        viewModel.todayUsage.observe(viewLifecycleOwner) { todayResult ->
            when (todayResult) {
                is TodayUsageResult.Success -> {
                    adapter.updateData(todayResult.summary.fullUsageList)
                }
                is TodayUsageResult.Empty -> {
                    adapter.updateData(todayResult.summary.fullUsageList)
                }
                is TodayUsageResult.Error -> {
                    adapter.updateData(emptyList())
                }
                null -> Unit
            }
            renderState()
        }

        binding.layoutState.btnRetry.setOnClickListener {
            viewModel.loadInsights()
        }
    }

    private fun renderState() {
        if (viewModel.isLoading.value == true) {
            showState(getString(R.string.insights_state_loading), loading = true)
            return
        }

        if (viewModel.hasLoadError.value == true) {
            showState(getString(R.string.insights_state_error), retry = true)
            return
        }

        val weeklyTrend = viewModel.weeklyTrend.value.orEmpty()
        val todayResult = viewModel.todayUsage.value
        val hasTrendData = weeklyTrend.any {
            it.totalScreenTimeMillis > 0L || it.weightedScreenTimeMillis > 0L
        }

        if (todayResult is TodayUsageResult.Error && !hasTrendData) {
            showState(getString(R.string.insights_state_error), retry = true)
            return
        }

        if (!hasTrendData && todayResult != null) {
            showState(getString(R.string.insights_state_empty))
            return
        }

        hideState()
    }

    private fun showState(message: String, loading: Boolean = false, retry: Boolean = false) {
        binding.layoutState.root.visibility = View.VISIBLE
        binding.layoutState.progressState.visibility = if (loading) View.VISIBLE else View.GONE
        binding.layoutState.tvState.text = message
        binding.layoutState.btnRetry.visibility = if (retry) View.VISIBLE else View.GONE

        binding.cardTrend.visibility = View.GONE
        binding.cardAnalytics.visibility = View.GONE
        binding.tvDailyBreakdownLabel.visibility = View.GONE
        binding.rvDailyBreakdown.visibility = View.GONE
    }

    private fun hideState() {
        binding.layoutState.root.visibility = View.GONE
        binding.cardTrend.visibility = View.VISIBLE
        binding.cardAnalytics.visibility = View.VISIBLE
        binding.tvDailyBreakdownLabel.visibility = View.VISIBLE
        binding.rvDailyBreakdown.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
