package com.studycompanion.app.feature.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studycompanion.app.core.ui.component.TimeFormatter
import com.studycompanion.app.core.ui.theme.StudyTheme
import com.studycompanion.app.domain.model.DaySummary
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun StudyHabitCalendarCard(
    currentMonth: YearMonth,
    daySummaries: Map<String, DaySummary>,
    selectedDateKey: String?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyTheme.colors
    val shapes = StudyTheme.shapes
    val typography = StudyTheme.typography

    val today = LocalDate.now()
    val todayKey = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
    val effectiveSelectedKey = selectedDateKey ?: todayKey

    // Calculate month stats
    val daysInMonth = currentMonth.lengthOfMonth()
    var studiedDaysCount = 0
    var goalMetDaysCount = 0
    var monthTotalSeconds = 0L

    for (day in 1..daysInMonth) {
        val date = currentMonth.atDay(day)
        val dKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val summary = daySummaries[dKey]
        if (summary != null && summary.totalSeconds > 0L) {
            studiedDaysCount++
            monthTotalSeconds += summary.totalSeconds
            if (summary.isTargetMet) {
                goalMetDaysCount++
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.large)
            .background(colors.surface)
            .padding(20.dp)
    ) {
        Column {
            // Month Header with Prev/Next Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Study Activity Calendar",
                        style = typography.bodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        style = typography.cardTitle.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPreviousMonth,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Previous Month",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onNextMonth,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Next Month",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Month High-level Metrics Pill Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.medium)
                    .background(colors.surfaceElevated)
                    .padding(vertical = 10.dp, horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$studiedDaysCount / $daysInMonth",
                        style = typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = colors.accent
                    )
                    Text(
                        text = "Days Studied",
                        style = typography.bodySmall.copy(fontSize = 11.sp),
                        color = colors.textSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(colors.borderSubtle)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$goalMetDaysCount",
                        style = typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF10B981)
                    )
                    Text(
                        text = "Goals Met",
                        style = typography.bodySmall.copy(fontSize = 11.sp),
                        color = colors.textSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(colors.borderSubtle)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = TimeFormatter.formatDurationShort(monthTotalSeconds),
                        style = typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Total Focus",
                        style = typography.bodySmall.copy(fontSize = 11.sp),
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Day-of-week initials (M, T, W, T, F, S, S)
            val weekInitials = listOf("M", "T", "W", "T", "F", "S", "S")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekInitials.forEach { initial ->
                    Text(
                        text = initial,
                        style = typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                        color = colors.textTertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Grid
            val firstDayOfMonth = currentMonth.atDay(1)
            // Day of week: Monday is 1, Sunday is 7 -> leading offset = dayOfWeek.value - 1
            val leadingOffset = firstDayOfMonth.dayOfWeek.value - 1
            val totalCells = leadingOffset + daysInMonth
            val totalRows = (totalCells + 6) / 7

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (row in 0 until totalRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (col in 0..6) {
                            val cellIndex = row * 7 + col
                            val dayNumber = cellIndex - leadingOffset + 1

                            if (dayNumber in 1..daysInMonth) {
                                val cellDate = currentMonth.atDay(dayNumber)
                                val cellKey = cellDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                val isToday = cellDate == today
                                val isFuture = cellDate.isAfter(today)
                                val isSelected = cellKey == effectiveSelectedKey
                                val summary = daySummaries[cellKey]
                                val totalSec = summary?.totalSeconds ?: 0L
                                val hasStudied = totalSec > 0L
                                val isGoalMet = summary?.isTargetMet == true

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(shapes.small)
                                        .background(
                                            when {
                                                isSelected -> colors.accent.copy(alpha = 0.22f)
                                                isToday -> colors.surfaceElevated
                                                else -> Color.Transparent
                                            }
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else if (isToday) 1.dp else 0.dp,
                                            color = if (isSelected) colors.accent else if (isToday) colors.borderSubtle else Color.Transparent,
                                            shape = shapes.small
                                        )
                                        .clickable(enabled = !isFuture) {
                                            onSelectDate(cellKey)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "$dayNumber",
                                            style = typography.bodySmall.copy(
                                                fontSize = 13.sp,
                                                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = when {
                                                isFuture -> colors.textTertiary.copy(alpha = 0.4f)
                                                isSelected -> colors.accent
                                                hasStudied -> colors.textPrimary
                                                else -> colors.textSecondary
                                            }
                                        )

                                        Spacer(modifier = Modifier.height(3.dp))

                                        // Status dot indicator
                                        if (!isFuture) {
                                            val dotColor = when {
                                                isGoalMet -> colors.accent // Bright emerald green
                                                hasStudied -> Color(0xFF38BDF8) // Soft cyan/blue for studied days
                                                else -> colors.borderSubtle // Subtle dark dot for unstudied past days
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(if (hasStudied) 5.dp else 3.5.dp)
                                                    .clip(CircleShape)
                                                    .background(dotColor)
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.size(5.dp))
                                        }
                                    }
                                }
                            } else {
                                // Empty cell before start or after end of month
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calendar Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Goal Met
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Goal Met",
                    style = typography.bodySmall.copy(fontSize = 11.sp),
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Studied
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF38BDF8))
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Studied",
                    style = typography.bodySmall.copy(fontSize = 11.sp),
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Rest / No Study
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colors.borderSubtle)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Rest / Missed",
                    style = typography.bodySmall.copy(fontSize = 11.sp),
                    color = colors.textSecondary
                )
            }

            // Selected Day Detail Box
            val selectedSummary = daySummaries[effectiveSelectedKey]
            val selectedDate = try {
                LocalDate.parse(effectiveSelectedKey)
            } catch (e: Exception) {
                today
            }

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.medium)
                    .background(colors.surfaceElevated)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                            style = typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if ((selectedSummary?.totalSeconds ?: 0L) > 0L) {
                                TimeFormatter.formatDurationShort(selectedSummary!!.totalSeconds)
                            } else {
                                "No study recorded"
                            },
                            style = typography.cardTitle.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if ((selectedSummary?.totalSeconds ?: 0L) > 0L) colors.textPrimary else colors.textTertiary
                            )
                        )
                        if ((selectedSummary?.sessionCount ?: 0) > 0) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${selectedSummary!!.sessionCount} focus session${if (selectedSummary.sessionCount > 1) "s" else ""}",
                                style = typography.bodySmall.copy(fontSize = 11.sp),
                                color = colors.textSecondary
                            )
                        }
                    }

                    // Target / Status badge
                    val targetSec = selectedSummary?.targetSeconds ?: 0L
                    val isMet = selectedSummary?.isTargetMet == true
                    val hasTime = (selectedSummary?.totalSeconds ?: 0L) > 0L

                    Box(
                        modifier = Modifier
                            .clip(shapes.pill)
                            .background(
                                when {
                                    isMet -> colors.accent.copy(alpha = 0.2f)
                                    hasTime -> Color(0xFF38BDF8).copy(alpha = 0.15f)
                                    else -> colors.surfaceVariant
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = when {
                                isMet -> "Goal Met ✓"
                                hasTime && targetSec > 0L -> "Target: ${TimeFormatter.formatDurationShort(targetSec)}"
                                hasTime -> "Studied"
                                selectedDate.isAfter(today) -> "Upcoming"
                                else -> "Rest Day"
                            },
                            style = typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                            color = when {
                                isMet -> colors.accent
                                hasTime -> Color(0xFF38BDF8)
                                else -> colors.textTertiary
                            }
                        )
                    }
                }
            }
        }
    }
}
