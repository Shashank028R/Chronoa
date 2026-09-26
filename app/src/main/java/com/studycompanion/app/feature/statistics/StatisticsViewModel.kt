package com.studycompanion.app.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studycompanion.app.domain.model.DaySummary
import com.studycompanion.app.domain.model.StudyStatsOverview
import com.studycompanion.app.domain.repository.ProfileRepository
import com.studycompanion.app.domain.repository.StatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class StatisticsUiState(
    val isLoading: Boolean = false,
    val statsOverview: StudyStatsOverview = StudyStatsOverview(),
    val daySummariesMap: Map<String, DaySummary> = emptyMap(),
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDateKey: String? = null
)

class StatisticsViewModel(
    private val profileRepository: ProfileRepository,
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StatisticsUiState(
            isLoading = true,
            selectedDateKey = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
    )
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private val todayDateKey: String
        get() = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    init {
        loadStatistics()
    }

    fun loadStatistics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val profile = profileRepository.getActiveProfile().first()
            if (profile != null) {
                // Collect stats overview
                launch {
                    statsRepository.observeStatsOverview(profile.id, todayDateKey).collect { stats ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            statsOverview = stats
                        )
                    }
                }
                // Collect all day summaries for calendar
                launch {
                    statsRepository.observeAllDaySummaries(profile.id).collect { summaries ->
                        _uiState.value = _uiState.value.copy(
                            daySummariesMap = summaries
                        )
                    }
                }
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun previousMonth() {
        _uiState.value = _uiState.value.copy(
            currentMonth = _uiState.value.currentMonth.minusMonths(1)
        )
    }

    fun nextMonth() {
        _uiState.value = _uiState.value.copy(
            currentMonth = _uiState.value.currentMonth.plusMonths(1)
        )
    }

    fun selectDate(dateKey: String) {
        _uiState.value = _uiState.value.copy(
            selectedDateKey = dateKey
        )
    }

    class Factory(
        private val profileRepository: ProfileRepository,
        private val statsRepository: StatsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatisticsViewModel(profileRepository, statsRepository) as T
        }
    }
}
