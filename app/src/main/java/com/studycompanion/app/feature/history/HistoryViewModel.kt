package com.studycompanion.app.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studycompanion.app.domain.model.DaySummary
import com.studycompanion.app.domain.model.StudyApp
import com.studycompanion.app.domain.model.StudySession
import com.studycompanion.app.domain.model.TrackingType
import com.studycompanion.app.domain.model.VerificationStatus
import com.studycompanion.app.domain.repository.ProfileRepository
import com.studycompanion.app.domain.repository.SessionRepository
import com.studycompanion.app.domain.repository.StatsRepository
import com.studycompanion.app.domain.repository.StudyAppRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

data class HistoryUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val daySummary: DaySummary = DaySummary("", 0L, 0L, 0L, 0L, false),
    val sessions: List<StudySession> = emptyList(),
    val appLabels: Map<String, String> = emptyMap(),
    val selectedSession: StudySession? = null,
    val showAddManualDialog: Boolean = false,
    val showSessionDetailDialog: Boolean = false
)

class HistoryViewModel(
    private val sessionRepository: SessionRepository,
    private val profileRepository: ProfileRepository,
    private val statsRepository: StatsRepository,
    private val studyAppRepository: StudyAppRepository,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadDataForDate(LocalDate.now())
    }

    fun selectDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        loadDataForDate(date)
    }

    fun selectPreviousDay() {
        selectDate(_uiState.value.selectedDate.minusDays(1))
    }

    fun previousDay() = selectPreviousDay()

    fun selectNextDay() {
        val next = _uiState.value.selectedDate.plusDays(1)
        if (!next.isAfter(LocalDate.now())) {
            selectDate(next)
        }
    }

    fun nextDay() = selectNextDay()

    private var loadJob: Job? = null

    private fun loadDataForDate(date: LocalDate) {
        val dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val profile = profileRepository.getActiveProfile().first() ?: return@launch
            val startOfDay = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endOfDay = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

            val allSessions = sessionRepository.getSessions(profile.id).first()
            val daySessions = allSessions.filter { s ->
                s.startAt in startOfDay..endOfDay
            }.sortedByDescending { it.startAt }

            val apps = studyAppRepository.getStudyAppsSync(profile.id)
            val labelMap = apps.associate { it.packageName to it.customLabel }.toMutableMap()
            labelMap["com.studycompanion.app"] = "Chronoa"
            labelMap["com.studycompanion.app.debug"] = "Chronoa"

            statsRepository.observeDaySummary(profile.id, dateKey).collect { summary ->
                _uiState.value = _uiState.value.copy(
                    selectedDate = date,
                    daySummary = summary,
                    sessions = daySessions,
                    appLabels = labelMap
                )
            }
        }
    }

    fun openSessionDetail(session: StudySession) {
        _uiState.value = _uiState.value.copy(
            selectedSession = session,
            showSessionDetailDialog = true
        )
    }

    fun closeSessionDetail() {
        _uiState.value = _uiState.value.copy(
            selectedSession = null,
            showSessionDetailDialog = false
        )
    }

    fun showAddManualDialog() {
        _uiState.value = _uiState.value.copy(showAddManualDialog = true)
    }

    fun hideAddManualDialog() {
        _uiState.value = _uiState.value.copy(showAddManualDialog = false)
    }

    fun addManualSession(
        startAt: Long,
        endAt: Long,
        subjectId: String? = null
    ) {
        val profile = _uiState.value.daySummary.dateKey
        viewModelScope.launch {
            val currentProfile = profileRepository.getActiveProfileSync()
                ?: profileRepository.getActiveProfile().first()
                ?: return@launch
            val durationSeconds = maxOf(0L, (endAt - startAt) / 1000L)
            val manualSession = StudySession(
                id = UUID.randomUUID().toString(),
                profileId = currentProfile.id,
                deviceId = "local-device",
                platform = "ANDROID",
                packageName = null,
                startAt = startAt,
                endAt = endAt,
                durationSeconds = durationSeconds,
                trackingType = TrackingType.MANUAL,
                verificationStatus = VerificationStatus.MANUAL_UNVERIFIED,
                subjectId = subjectId?.takeIf { it.isNotBlank() },
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            sessionRepository.recordSession(manualSession)
            hideAddManualDialog()
            loadDataForDate(_uiState.value.selectedDate)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            sessionRepository.deleteSession(sessionId)
            closeSessionDetail()
            loadDataForDate(_uiState.value.selectedDate)
        }
    }

    class Factory(
        private val sessionRepository: SessionRepository,
        private val profileRepository: ProfileRepository,
        private val statsRepository: StatsRepository,
        private val studyAppRepository: StudyAppRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(
                sessionRepository,
                profileRepository,
                statsRepository,
                studyAppRepository
            ) as T
        }
    }
}
