package com.hsissa.zentra.ui.insights

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hsissa.zentra.data.repository.UsageRepository
import com.hsissa.zentra.service.DailyUsageSummary
import com.hsissa.zentra.service.TodayUsageResult
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(private val repository: UsageRepository) : ViewModel() {

    private val _weeklyTrend = MutableLiveData<List<DailyUsageSummary>>()
    val weeklyTrend: LiveData<List<DailyUsageSummary>> = _weeklyTrend

    private val _todayUsage = MutableLiveData<TodayUsageResult>()
    val todayUsage: LiveData<TodayUsageResult> = _todayUsage

    fun loadInsights() {
        viewModelScope.launch {
            _weeklyTrend.value = repository.getWeeklyTrend()
            _todayUsage.value = repository.getTodayUsage()
        }
    }
}
