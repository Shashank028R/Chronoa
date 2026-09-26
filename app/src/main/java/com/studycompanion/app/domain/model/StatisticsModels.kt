package com.studycompanion.app.domain.model

data class AppStudyTime(
    val packageName: String,
    val appLabel: String,
    val totalSeconds: Long
)

data class DaySummary(
    val dateKey: String, // YYYY-MM-DD
    val automaticSeconds: Long,
    val manualSeconds: Long,
    val totalSeconds: Long,
    val targetSeconds: Long,
    val isTargetMet: Boolean,
    val sessionCount: Int = 0
)

data class WeeklyStats(
    val totalSeconds: Long,
    val dailyBreakdown: List<DaySummary>,
    val completionRate: Float,
    val topApps: List<AppStudyTime>
)

data class MonthlyStats(
    val totalSeconds: Long,
    val averageDailySeconds: Long,
    val bestDayDateKey: String?,
    val bestDaySeconds: Long,
    val totalSessionsCount: Int
)

data class StudyStatsOverview(
    val todayAutomaticSeconds: Long = 0L,
    val todayManualSeconds: Long = 0L,
    val todayTotalSeconds: Long = 0L,
    val todayTargetSeconds: Long = 0L,
    val todayRemainingSeconds: Long = 0L,
    val todaySessionCount: Int = 0,
    val todayLongestSessionSeconds: Long = 0L,
    val currentStreakDays: Int = 0,
    val weeklyStats: WeeklyStats = WeeklyStats(0L, emptyList(), 0f, emptyList()),
    val monthlyStats: MonthlyStats = MonthlyStats(0L, 0L, null, 0L, 0),
    val topStudyApps: List<AppStudyTime> = emptyList()
)
