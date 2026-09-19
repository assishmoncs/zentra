package com.hsissa.zentra.ui.insights

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
class InsightsViewModel @Inject constructor(
    private val getTodayUsage: GetTodayUsageUseCase,
    private val getWeeklyTrend: GetWeeklyTrendUseCase
) : ViewModel() {

    private val _weeklyTrend = MutableLiveData<List<DailyUsageSummary>?>(emptyList())
    val weeklyTrend: LiveData<List<DailyUsageSummary>?> = _weeklyTrend

    private val _todayUsage = MutableLiveData<TodayUsageResult?>(null)
    val todayUsage: LiveData<TodayUsageResult?> = _todayUsage

    private val _isLoading = MutableLiveData<Boolean?>(false)
    val isLoading: LiveData<Boolean?> = _isLoading

    private val _hasLoadError = MutableLiveData<Boolean?>(false)
    val hasLoadError: LiveData<Boolean?> = _hasLoadError

    fun loadInsights() {
        viewModelScope.launch {
            _isLoading.value = true
            _hasLoadError.value = false
            try {
                _weeklyTrend.value = getWeeklyTrend()
                _todayUsage.value = getTodayUsage()
            } catch (_: Exception) {
                _hasLoadError.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }
}
