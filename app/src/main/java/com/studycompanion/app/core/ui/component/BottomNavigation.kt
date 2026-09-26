package com.studycompanion.app.core.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.studycompanion.app.core.ui.theme.StudyTheme

enum class MainDestination(val label: String) {
    HOME("Home"),
    HISTORY("History"),
    STATISTICS("Statistics"),
    SETTINGS("Settings")
}

@Composable
fun StudyBottomNavigation(
    currentDestination: MainDestination,
    onNavigate: (MainDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyTheme.colors
    val typography = StudyTheme.typography

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
    ) {
        // Subtle top border divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(colors.borderSubtle)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainDestination.entries.forEach { destination ->
                val isSelected = destination == currentDestination
                val icon = when (destination) {
                    MainDestination.HOME -> Icons.Filled.Home
                    MainDestination.HISTORY -> Icons.Filled.Refresh
                    MainDestination.STATISTICS -> Icons.Filled.Info
                    MainDestination.SETTINGS -> Icons.Filled.Settings
                }

                val itemColor by animateColorAsState(
                    targetValue = if (isSelected) colors.textPrimary else colors.textTertiary,
                    label = "navColor"
                )

                val interactionSource = remember { MutableInteractionSource() }

                Column(
                    modifier = Modifier
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onNavigate(destination) }
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = destination.label,
                        tint = itemColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = destination.label,
                        style = typography.labelMedium,
                        color = itemColor,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }
    }
}
