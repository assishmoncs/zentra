package com.hsissa.zentra.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hsissa.zentra.service.DailyUsageSummary
import com.hsissa.zentra.service.TodayUsageResult
import com.hsissa.zentra.data.repository.UsageRepository
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(private val repository: UsageRepository) : ViewModel() {

    private val _todayUsage = MutableLiveData<TodayUsageResult>()
    val todayUsage: LiveData<TodayUsageResult> = _todayUsage

    private val _weeklyTrend = MutableLiveData<List<DailyUsageSummary>>()
    val weeklyTrend: LiveData<List<DailyUsageSummary>> = _weeklyTrend

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            val today = repository.getTodayUsage()
            _todayUsage.value = today
            
            val weekly = repository.getWeeklyTrend()
            _weeklyTrend.value = weekly
            
            _isLoading.value = false
        }
    }
}
