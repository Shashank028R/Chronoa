package com.studycompanion.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studycompanion.app.core.ui.component.SyncStatusIndicator
import com.studycompanion.app.core.ui.theme.AppThemeMode
import com.studycompanion.app.core.ui.theme.StudyTheme
import com.studycompanion.app.sync.model.SyncState

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onNavigateStudyApps: () -> Unit,
    onNavigateHealthCenter: () -> Unit,
    onSelectTheme: (AppThemeMode) -> Unit,
    onToggleCountWhileLocked: (Boolean) -> Unit,
    onTogglePauseDuringCalls: (Boolean) -> Unit,
    onTogglePauseInMultiWindow: (Boolean) -> Unit,
    onTriggerSync: () -> Unit,
    onSwitchProfile: () -> Unit,
    onLockProfile: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyTheme.colors
    val shapes = StudyTheme.shapes
    val typography = StudyTheme.typography
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 40.dp)
    ) {
        Text(
            text = "Settings",
            style = typography.screenTitle,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION: FOCUS RULES
        Text(
            text = "FOCUS RULES",
            style = typography.sectionHeader,
            color = colors.textTertiary
        )
        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.medium)
                .background(colors.surface)
        ) {
            Column {
                SettingsActionRow(
                    title = "Approved Study Apps",
                    subtitle = "Manage apps that qualify as study time",
                    onClick = onNavigateStudyApps
                )
                SettingsDivider()
                SettingsToggleRow(
                    title = "Count while screen is locked",
                    subtitle = "Track audio or reading apps when display is off",
                    checked = state.settings.countWhileLocked,
                    onCheckedChange = onToggleCountWhileLocked
                )
                SettingsDivider()
                SettingsToggleRow(
                    title = "Pause during calls",
                    subtitle = "Automatically pause study timer during phone calls",
                    checked = state.settings.pauseDuringCalls,
                    onCheckedChange = onTogglePauseDuringCalls
                )
                SettingsDivider()
                SettingsToggleRow(
                    title = "Pause in multi-window",
                    subtitle = "Pause if unapproved apps share the screen",
                    checked = state.settings.pauseInMultiWindow,
                    onCheckedChange = onTogglePauseInMultiWindow
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // SECTION: APPEARANCE
        Text(
            text = "APPEARANCE",
            style = typography.sectionHeader,
            color = colors.textTertiary
        )
        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.medium)
                .background(colors.surface)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Display Theme",
                    style = typography.cardTitle,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ThemeOptionChip("System", AppThemeMode.SYSTEM, state.currentTheme, onSelectTheme)
                    ThemeOptionChip("Light", AppThemeMode.LIGHT, state.currentTheme, onSelectTheme)
                    ThemeOptionChip("Dark", AppThemeMode.DARK, state.currentTheme, onSelectTheme)
                    ThemeOptionChip("AMOLED", AppThemeMode.AMOLED, state.currentTheme, onSelectTheme)
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // SECTION: PERMISSIONS & HEALTH
        Text(
            text = "SYSTEM & DIAGNOSTICS",
            style = typography.sectionHeader,
            color = colors.textTertiary
        )
        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.medium)
                .background(colors.surface)
        ) {
            SettingsActionRow(
                title = "Permission & Health Center",
                subtitle = "Check Usage Access, battery optimizations, and tracking health",
                leadingIcon = Icons.Default.Info,
                onClick = onNavigateHealthCenter
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // SECTION: ACCOUNT & PROFILES
        Text(
            text = "ACCOUNT & PROFILES",
            style = typography.sectionHeader,
            color = colors.textTertiary
        )
        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.medium)
                .background(colors.surface)
        ) {
            Column {
                SettingsActionRow(
                    title = "Active Profile: ${state.activeProfile?.name ?: "None"}",
                    subtitle = state.currentUser?.email ?: "Account logged in",
                    onClick = onSwitchProfile
                )
                SettingsDivider()
                SettingsActionRow(
                    title = "Lock Profile",
                    subtitle = "Require PIN verification to re-enter",
                    leadingIcon = Icons.Default.Lock,
                    onClick = onLockProfile
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // SECTION: CLOUD SYNCHRONIZATION
        Text(
            text = "CLOUD BACKUP & SYNC",
            style = typography.sectionHeader,
            color = colors.textTertiary
        )
        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.medium)
                .background(colors.surface)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cloud Sync",
                        style = typography.cardTitle,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    SyncStatusIndicator(syncState = state.syncState)
                }

                if (state.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = colors.accent
                    )
                } else {
                    OutlinedButton(
                        onClick = onTriggerSync,
                        shape = shapes.pill
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sync Now", color = colors.textPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Log out button
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = shapes.pill,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.statusError),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(colors.statusError.copy(alpha = 0.5f))
            )
        ) {
            Text("Log Out", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Chronoa Branding / About Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.medium)
                .background(colors.surface)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.studycompanion.app.R.drawable.ic_chronoa_logo),
                    contentDescription = "Chronoa Logo",
                    modifier = Modifier
                        .size(54.dp)
                        .clip(shapes.medium)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Chronoa",
                    style = typography.cardTitle.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Smart Focus & Study Habit Tracker • v1.0.0",
                    style = typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionChip(
    label: String,
    mode: AppThemeMode,
    currentSelected: AppThemeMode,
    onSelect: (AppThemeMode) -> Unit
) {
    val colors = StudyTheme.colors
    val shapes = StudyTheme.shapes
    val isSelected = currentSelected == mode

    Box(
        modifier = Modifier
            .clip(shapes.pill)
            .background(if (isSelected) colors.accent else colors.surfaceElevated)
            .clickable { onSelect(mode) }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) colors.background else colors.textPrimary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun SettingsDivider() {
    val colors = StudyTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(colors.borderSubtle)
    )
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    val colors = StudyTheme.colors
    val typography = StudyTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column {
                Text(
                    text = title,
                    style = typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = StudyTheme.colors
    val typography = StudyTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = typography.bodySmall,
                color = colors.textSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.background,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = colors.textSecondary,
                uncheckedTrackColor = colors.surfaceElevated
            )
        )
    }
}
