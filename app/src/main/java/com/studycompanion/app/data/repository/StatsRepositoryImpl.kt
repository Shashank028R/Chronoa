package com.studycompanion.app.data.repository

import com.studycompanion.app.core.database.dao.DailyTargetDao
import com.studycompanion.app.core.database.dao.StudyAppDao
import com.studycompanion.app.core.database.dao.StudySessionDao
import com.studycompanion.app.domain.model.AppStudyTime
import com.studycompanion.app.domain.model.DaySummary
import com.studycompanion.app.domain.model.MonthlyStats
import com.studycompanion.app.domain.model.StudyStatsOverview
import com.studycompanion.app.domain.model.WeeklyStats
import com.studycompanion.app.domain.repository.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class StatsRepositoryImpl(
    private val sessionDao: StudySessionDao,
    private val targetDao: DailyTargetDao,
    private val studyAppDao: StudyAppDao,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : StatsRepository {

    override fun observeStatsOverview(
        profileId: String,
        todayDateKey: String
    ): Flow<StudyStatsOverview> {
        return combine(
            sessionDao.getSessionsFlowForProfile(profileId),
            targetDao.getTargetsFlowForProfile(profileId),
            studyAppDao.getAppsFlowForProfile(profileId)
        ) { sessions, targets, apps ->
            computeOverview(todayDateKey, sessions, targets, apps)
        }
    }

    private fun computeOverview(
        todayDateKey: String,
        sessions: List<com.studycompanion.app.core.database.entity.StudySessionEntity>,
        targets: List<com.studycompanion.app.core.database.entity.DailyTargetEntity>,
        apps: List<com.studycompanion.app.core.database.entity.StudyAppEntity>
    ): StudyStatsOverview {
        val appLabels = apps.associate { it.packageName to it.appLabel }
        val targetMap = targets.associate { it.dateKey to (it.adjustedTargetSeconds ?: it.originalTargetSeconds) }

            // Group sessions by local dateKey
            val sessionsByDate = sessions.groupBy { session ->
                val date = Instant.ofEpochMilli(session.startAt).atZone(zoneId).toLocalDate()
                date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            }

            // 1. Today metrics
            val todaySessions = sessionsByDate[todayDateKey] ?: emptyList()
            var todayAuto = 0L
            var todayManual = 0L
            var todayLongest = 0L

            for (s in todaySessions) {
                if (s.trackingType == "AUTOMATIC") {
                    todayAuto += s.durationSeconds
                    if (s.durationSeconds > todayLongest) {
                        todayLongest = s.durationSeconds
                    }
                } else {
                    todayManual += s.durationSeconds
                }
            }
            val todayTotal = todayAuto + todayManual
            val todayTarget = targetMap[todayDateKey] ?: 0L
            val todayRemaining = maxOf(0L, todayTarget - todayAuto)

            // 2. Weekly stats (last 7 days up to today)
            val todayDate = try { LocalDate.parse(todayDateKey) } catch (e: Exception) { LocalDate.now() }
            val weeklySummaries = mutableListOf<DaySummary>()
            var weeklyTotal = 0L
            var metDaysCount = 0

            for (i in 6 downTo 0) {
                val d = todayDate.minusDays(i.toLong())
                val dKey = d.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val daySessions = sessionsByDate[dKey] ?: emptyList()

                var auto = 0L
                var manual = 0L
                for (s in daySessions) {
                    if (s.trackingType == "AUTOMATIC") auto += s.durationSeconds
                    else manual += s.durationSeconds
                }
                val total = auto + manual
                val target = targetMap[dKey] ?: 0L
                val met = target > 0L && auto >= target
                if (met) metDaysCount++
                weeklyTotal += total

                weeklySummaries.add(
                    DaySummary(
                        dateKey = dKey,
                        automaticSeconds = auto,
                        manualSeconds = manual,
                        totalSeconds = total,
                        targetSeconds = target,
                        isTargetMet = met,
                        sessionCount = daySessions.size
                    )
                )
            }

            // Top apps (accumulated automatic time)
            val appTotals = mutableMapOf<String, Long>()
            for (s in sessions) {
                val pkg = s.packageName
                if (pkg != null && s.trackingType == "AUTOMATIC") {
                    appTotals[pkg] = (appTotals[pkg] ?: 0L) + s.durationSeconds
                }
            }
            val topApps = appTotals.entries
                .sortedByDescending { it.value }
                .take(5)
                .map { entry ->
                    val isOwnApp = entry.key == "com.studycompanion.app" ||
                        entry.key == "com.studycompanion.app.debug" ||
                        entry.key.contains("studycompanion")
                    val label = when {
                        isOwnApp -> "Chronoa"
                        appLabels.containsKey(entry.key) && !appLabels[entry.key].isNullOrBlank() && !appLabels[entry.key]!!.equals("debug", ignoreCase = true) -> appLabels[entry.key]!!
                        else -> {
                            val last = entry.key.substringAfterLast('.')
                            if (last.equals("debug", ignoreCase = true) || last.isBlank()) "Chronoa" else last
                        }
                    }
                    AppStudyTime(
                        packageName = entry.key,
                        appLabel = label,
                        totalSeconds = entry.value
                    )
                }

            val weeklyRate = if (weeklySummaries.isNotEmpty()) metDaysCount.toFloat() / 7f else 0f
            val weeklyStats = WeeklyStats(
                totalSeconds = weeklyTotal,
                dailyBreakdown = weeklySummaries,
                completionRate = weeklyRate,
                topApps = topApps
            )

            // 3. Monthly stats (sessions in current month)
            val currentYearMonth = todayDate.year * 100 + todayDate.monthValue
            val monthlySessions = sessions.filter { s ->
                val date = Instant.ofEpochMilli(s.startAt).atZone(zoneId).toLocalDate()
                (date.year * 100 + date.monthValue) == currentYearMonth
            }

            var monthTotal = 0L
            val monthDaysMap = mutableMapOf<String, Long>()
            for (s in monthlySessions) {
                monthTotal += s.durationSeconds
                val dKey = Instant.ofEpochMilli(s.startAt).atZone(zoneId).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                monthDaysMap[dKey] = (monthDaysMap[dKey] ?: 0L) + s.durationSeconds
            }

            val dayOfMonth = todayDate.dayOfMonth
            val avgDaily = if (dayOfMonth > 0) monthTotal / dayOfMonth else 0L
            val bestDayEntry = monthDaysMap.maxByOrNull { it.value }

            val monthlyStats = MonthlyStats(
                totalSeconds = monthTotal,
                averageDailySeconds = avgDaily,
                bestDayDateKey = bestDayEntry?.key,
                bestDaySeconds = bestDayEntry?.value ?: 0L,
                totalSessionsCount = monthlySessions.size
            )

            // 4. Streak calculation (consecutive past days where auto >= target)
            var streak = 0
            var checkDate = todayDate
            // If today's target is already met, count today in streak
            val todayMet = todayTarget > 0L && todayAuto >= todayTarget
            if (todayMet) {
                streak++
                checkDate = checkDate.minusDays(1)
            } else {
                // If today is not yet met, check streak ending yesterday
                checkDate = checkDate.minusDays(1)
            }

            while (true) {
                val dKey = checkDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val target = targetMap[dKey] ?: 0L
                val daySessions = sessionsByDate[dKey] ?: emptyList()
                val auto = daySessions.filter { it.trackingType == "AUTOMATIC" }.sumOf { it.durationSeconds }

                if (target > 0L && auto >= target) {
                    streak++
                    checkDate = checkDate.minusDays(1)
                } else {
                    break
                }
            }

            return StudyStatsOverview(
                todayAutomaticSeconds = todayAuto,
                todayManualSeconds = todayManual,
                todayTotalSeconds = todayTotal,
                todayTargetSeconds = todayTarget,
                todayRemainingSeconds = todayRemaining,
                todaySessionCount = todaySessions.size,
                todayLongestSessionSeconds = todayLongest,
                currentStreakDays = streak,
                weeklyStats = weeklyStats,
                monthlyStats = monthlyStats,
                topStudyApps = topApps
            )
        }

    override fun observeDaySummary(profileId: String, dateKey: String): Flow<DaySummary> {
        return combine(
            sessionDao.getSessionsFlowForProfile(profileId),
            targetDao.getTargetFlow(profileId, dateKey)
        ) { sessions, targetEntity ->
            val daySessions = sessions.filter { session ->
                val date = Instant.ofEpochMilli(session.startAt).atZone(zoneId).toLocalDate()
                date.format(DateTimeFormatter.ISO_LOCAL_DATE) == dateKey
            }

            var auto = 0L
            var manual = 0L
            for (s in daySessions) {
                if (s.trackingType == "AUTOMATIC") auto += s.durationSeconds
                else manual += s.durationSeconds
            }
            val total = auto + manual
            val target = targetEntity?.adjustedTargetSeconds ?: targetEntity?.originalTargetSeconds ?: 0L
            val met = target > 0L && auto >= target

            DaySummary(
                dateKey = dateKey,
                automaticSeconds = auto,
                manualSeconds = manual,
                totalSeconds = total,
                targetSeconds = target,
                isTargetMet = met,
                sessionCount = daySessions.size
            )
        }
    }

    override fun observeAllDaySummaries(profileId: String): Flow<Map<String, DaySummary>> {
        return combine(
            sessionDao.getSessionsFlowForProfile(profileId),
            targetDao.getTargetsFlowForProfile(profileId)
        ) { sessions, targets ->
            val targetMap = targets.associate { it.dateKey to (it.adjustedTargetSeconds ?: it.originalTargetSeconds) }
            val sessionsByDate = sessions.groupBy { session ->
                val date = Instant.ofEpochMilli(session.startAt).atZone(zoneId).toLocalDate()
                date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            }

            val allDates = (sessionsByDate.keys + targetMap.keys).toSet()
            val result = mutableMapOf<String, DaySummary>()
            for (dKey in allDates) {
                val daySessions = sessionsByDate[dKey] ?: emptyList()
                var auto = 0L
                var manual = 0L
                for (s in daySessions) {
                    if (s.trackingType == "AUTOMATIC") auto += s.durationSeconds
                    else manual += s.durationSeconds
                }
                val total = auto + manual
                val target = targetMap[dKey] ?: 0L
                val met = target > 0L && auto >= target
                result[dKey] = DaySummary(
                    dateKey = dKey,
                    automaticSeconds = auto,
                    manualSeconds = manual,
                    totalSeconds = total,
                    targetSeconds = target,
                    isTargetMet = met,
                    sessionCount = daySessions.size
                )
            }
            result
        }
    }

    override suspend fun getStatsOverview(
        profileId: String,
        todayDateKey: String
    ): StudyStatsOverview {
        val sessions = sessionDao.getSessionsForProfile(profileId)
        val targets = targetDao.getTargetsForProfile(profileId)
        val apps = studyAppDao.getAppsForProfile(profileId)
        return computeOverview(todayDateKey, sessions, targets, apps)
    }
}
