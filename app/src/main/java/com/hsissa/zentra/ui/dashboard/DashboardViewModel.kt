package com.hsissa.zentra.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hsissa.zentra.domain.usecase.GetTodayUsageUseCase
import com.hsissa.zentra.domain.usecase.GetWeeklyTrendUseCase
import com.hsissa.zentra.service.DailyUsageSummary
import com.hsissa.zentra.service.TodayUsageResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getTodayUsage: GetTodayUsageUseCase,
    private val getWeeklyTrend: GetWeeklyTrendUseCase
) : ViewModel() {

    private val _todayUsage = MutableLiveData<TodayUsageResult>()
    val todayUsage: LiveData<TodayUsageResult> = _todayUsage

    private val _weeklyTrend = MutableLiveData<List<DailyUsageSummary>>()
    val weeklyTrend: LiveData<List<DailyUsageSummary>> = _weeklyTrend

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _todayUsage.value = getTodayUsage()
                _weeklyTrend.value = getWeeklyTrend()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
