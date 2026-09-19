package com.hsissa.zentra.domain.usecase

import com.hsissa.zentra.domain.repository.UsageRepositoryContract
import com.hsissa.zentra.service.DailyUsageSummary
import javax.inject.Inject

class GetWeeklyTrendUseCase @Inject constructor(
    private val repository: UsageRepositoryContract
) {
    suspend operator fun invoke(): List<DailyUsageSummary> = repository.getWeeklyTrend()
}
