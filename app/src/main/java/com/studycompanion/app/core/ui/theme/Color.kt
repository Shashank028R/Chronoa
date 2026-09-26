package com.studycompanion.app.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic color palette for Study Companion.
 * Supports Light, Dark, and AMOLED (true black #000000) modes with restrained, Apple-inspired accents.
 */
@Immutable
data class StudyColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceVariant: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val border: Color,
    val borderSubtle: Color,
    val accent: Color,
    val accentSubtle: Color,
    val statusStudying: Color,
    val statusPaused: Color,
    val statusIdle: Color,
    val statusWarning: Color,
    val statusError: Color,
    val isAmoled: Boolean = false
)

// AMOLED Theme (True #000000 Black for distraction-free, low-power focus)
val AmoledStudyColors = StudyColors(
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    surfaceElevated = Color(0xFF141414),
    surfaceVariant = Color(0xFF1C1C1E),
    textPrimary = Color(0xFFF5F5F7),
    textSecondary = Color(0xFF8E8E93),
    textTertiary = Color(0xFF636366),
    border = Color(0xFF2C2C2E),
    borderSubtle = Color(0xFF1A1A1C),
    accent = Color(0xFF10B981), // Emerald
    accentSubtle = Color(0x1A10B981),
    statusStudying = Color(0xFF34D399),
    statusPaused = Color(0xFFFBBF24),
    statusIdle = Color(0xFF9CA3AF),
    statusWarning = Color(0xFFF59E0B),
    statusError = Color(0xFFF87171),
    isAmoled = true
)

// Dark Theme (Refined deep slate for comfortable nighttime use)
val DarkStudyColors = StudyColors(
    background = Color(0xFF0B0F17),
    surface = Color(0xFF131A26),
    surfaceElevated = Color(0xFF1C2433),
    surfaceVariant = Color(0xFF263042),
    textPrimary = Color(0xFFF1F5F9),
    textSecondary = Color(0xFF94A3B8),
    textTertiary = Color(0xFF64748B),
    border = Color(0xFF1E293B),
    borderSubtle = Color(0xFF151E2E),
    accent = Color(0xFF10B981),
    accentSubtle = Color(0x2210B981),
    statusStudying = Color(0xFF10B981),
    statusPaused = Color(0xFFF59E0B),
    statusIdle = Color(0xFF64748B),
    statusWarning = Color(0xFFF59E0B),
    statusError = Color(0xFFEF4444),
    isAmoled = false
)

// Light Theme (Soft neutral, airy Apple-inspired light appearance)
val LightStudyColors = StudyColors(
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFFE2E8F0),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textTertiary = Color(0xFF94A3B8),
    border = Color(0xFFE2E8F0),
    borderSubtle = Color(0xFFF1F5F9),
    accent = Color(0xFF059669),
    accentSubtle = Color(0x15059669),
    statusStudying = Color(0xFF059669),
    statusPaused = Color(0xFFD97706),
    statusIdle = Color(0xFF64748B),
    statusWarning = Color(0xFFD97706),
    statusError = Color(0xFFDC2626),
    isAmoled = false
)

val LocalStudyColors = staticCompositionLocalOf { DarkStudyColors }
