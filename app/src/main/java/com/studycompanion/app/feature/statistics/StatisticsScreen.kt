package com.studycompanion.app.feature.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studycompanion.app.core.ui.component.TimeFormatter
import com.studycompanion.app.core.ui.theme.StudyTheme
import com.studycompanion.app.domain.model.AppStudyTime
import com.studycompanion.app.domain.model.DaySummary
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun StatisticsScreen(
    state: StatisticsUiState,
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onSelectDate: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = StudyTheme.colors
    val shapes = StudyTheme.shapes
    val typography = StudyTheme.typography

    val stats = state.statsOverview
    val scrollState = rememberScrollState()

    val isEmpty = stats.weeklyStats.totalSeconds == 0L && stats.todayTotalSeconds == 0L

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 32.dp)
    ) {
        Text(
            text = "Personal Insights",
            style = typography.screenTitle,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Weekly Focused Card (Week Analysis)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.large)
                .background(colors.surface)
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "This Week's Focus",
                    style = typography.bodySmall,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = TimeFormatter.formatDurationShort(stats.weeklyStats.totalSeconds),
                    style = typography.timerHero.copy(fontSize = 44.sp, lineHeight = 50.sp),
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 7-day mini bar breakdown
                WeeklyBarGraph(
                    days = stats.weeklyStats.dailyBreakdown
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Full Month Habit Calendar ("which day i studied and which day i didn't")
        StudyHabitCalendarCard(
            currentMonth = state.currentMonth,
            daySummaries = state.daySummariesMap,
            selectedDateKey = state.selectedDateKey,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onSelectDate = onSelectDate
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 2x2 Metric Grid (Streak, Longest Session, Monthly Total, Avg/Day)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InsightMetricCard(
                title = "Current Streak",
                value = "${stats.currentStreakDays} days",
                subtitle = if (stats.currentStreakDays > 0) "Consistent habit" else "Start today",
                modifier = Modifier.weight(1f)
            )
            InsightMetricCard(
                title = "Longest Session",
                value = TimeFormatter.formatDurationShort(stats.todayLongestSessionSeconds),
                subtitle = "Deep focus",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InsightMetricCard(
                title = "This Month",
                value = TimeFormatter.formatDurationShort(stats.monthlyStats.totalSeconds),
                subtitle = "${stats.monthlyStats.totalSessionsCount} sessions",
                modifier = Modifier.weight(1f)
            )
            InsightMetricCard(
                title = "Daily Average",
                value = TimeFormatter.formatDurationShort(stats.monthlyStats.averageDailySeconds),
                subtitle = "Per active day",
                modifier = Modifier.weight(1f)
            )
        }

        // Top Study Apps Section
        if (stats.topStudyApps.isNotEmpty()) {
            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "TOP STUDY APPS",
                style = typography.sectionHeader,
                color = colors.textTertiary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.medium)
                    .background(colors.surface)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    val maxAppTime = stats.topStudyApps.maxOfOrNull { it.totalSeconds } ?: 1L
                    stats.topStudyApps.forEach { app ->
                        AppProgressRow(app = app, maxSeconds = maxAppTime)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyBarGraph(
    days: List<DaySummary>
) {
    val colors = StudyTheme.colors
    val shapes = StudyTheme.shapes
    val typography = StudyTheme.typography

    val maxDaySeconds = maxOf(3600L, days.maxOfOrNull { it.totalSeconds } ?: 3600L)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { day ->
            val date = try { LocalDate.parse(day.dateKey) } catch (e: Exception) { LocalDate.now() }
            val dayInitial = date.dayOfWeek.name.take(1)
            val barHeightFraction = (day.totalSeconds.toFloat() / maxDaySeconds.toFloat()).coerceIn(0.06f, 1f)

            val barColor = when {
                day.isTargetMet -> colors.accent
                day.totalSeconds > 0L -> colors.surfaceVariant
                else -> colors.borderSubtle
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .fillMaxHeight(barHeightFraction)
                        .clip(shapes.pill)
                        .background(barColor)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = dayInitial,
                    style = typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = if (date == LocalDate.now()) colors.textPrimary else colors.textTertiary
                )
            }
        }
    }
}

@Composable
private fun InsightMetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val colors = StudyTheme.colors
    val shapes = StudyTheme.shapes
    val typography = StudyTheme.typography

    Box(
        modifier = modifier
            .clip(shapes.medium)
            .background(colors.surface)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = title,
                style = typography.bodySmall,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = typography.cardTitle.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = typography.bodySmall.copy(fontSize = 11.sp),
                color = colors.textTertiary
            )
        }
    }
}

@Composable
private fun AppProgressRow(
    app: AppStudyTime,
    maxSeconds: Long
) {
    val colors = StudyTheme.colors
    val shapes = StudyTheme.shapes
    val typography = StudyTheme.typography

    val fraction = (app.totalSeconds.toFloat() / maxSeconds.toFloat()).coerceIn(0.04f, 1f)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = app.appLabel,
                style = typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = colors.textPrimary
            )
            Text(
                text = TimeFormatter.formatDurationShort(app.totalSeconds),
                style = typography.bodySmall,
                color = colors.textSecondary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(shapes.pill)
                .background(colors.borderSubtle)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(shapes.pill)
                    .background(colors.accent)
            )
        }
    }
}
