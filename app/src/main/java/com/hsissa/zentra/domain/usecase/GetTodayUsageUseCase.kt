package com.hsissa.zentra.domain.usecase

import com.hsissa.zentra.domain.repository.UsageRepositoryContract
import com.hsissa.zentra.service.TodayUsageResult
import javax.inject.Inject

class GetTodayUsageUseCase @Inject constructor(
    private val repository: UsageRepositoryContract
) {
    suspend operator fun invoke(): TodayUsageResult = repository.getTodayUsage()
}
