package com.studycompanion.app.feature.studyapps

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studycompanion.app.core.ui.theme.StudyTheme
import com.studycompanion.app.domain.model.StudyApp
import com.studycompanion.app.domain.repository.SelectableApp

@Composable
fun StudyAppManagerScreen(
    state: StudyAppManagerUiState,
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAddApp: (packageName: String, label: String) -> Unit,
    onRemoveApp: (packageName: String) -> Unit,
    onSetAppEnabled: (packageName: String, enabled: Boolean) -> Unit,
    onStartEditingApp: (StudyApp) -> Unit,
    onCancelEditingApp: () -> Unit,
    onSaveAppLabel: (packageName: String, newLabel: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyTheme.colors
    val shapes = StudyTheme.shapes
    val typography = StudyTheme.typography

    val approvedPackages = remember(state.approvedApps) {
        state.approvedApps.map { it.packageName }.toSet()
    }

    val filteredAvailable = remember(state.availableApps, state.searchQuery, approvedPackages) {
        state.availableApps.filter { app ->
            !approvedPackages.contains(app.packageName) &&
                    (state.searchQuery.isBlank() ||
                            app.label.contains(state.searchQuery, ignoreCase = true) ||
                            app.packageName.contains(state.searchQuery, ignoreCase = true))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp)
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Study Apps",
                style = typography.screenTitle,
                color = colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search installed apps…", color = colors.textTertiary) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = colors.textSecondary
                )
            },
            singleLine = true,
            shape = shapes.pill,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.borderSubtle,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Approved Study Apps Header
            item {
                Text(
                    text = "APPROVED STUDY APPS (${state.approvedApps.size})",
                    style = typography.sectionHeader,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (state.approvedApps.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.medium)
                            .background(colors.surface)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No study apps selected yet.\nChoose apps below that count as study time.",
                            style = typography.bodyMedium,
                            color = colors.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(state.approvedApps, key = { it.packageName }) { app ->
                    ApprovedAppItem(
                        app = app,
                        onToggle = { enabled -> onSetAppEnabled(app.packageName, enabled) },
                        onEdit = { onStartEditingApp(app) },
                        onRemove = { onRemoveApp(app.packageName) }
                    )
                }
            }

            // Available Installed Apps
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "AVAILABLE APPS (${filteredAvailable.size})",
                    style = typography.sectionHeader,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            items(filteredAvailable, key = { it.packageName }) { app ->
                AvailableAppItem(
                    app = app,
                    onAdd = { onAddApp(app.packageName, app.label) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Edit Label Dialog
    if (state.editingApp != null) {
        var newLabelText by remember { mutableStateOf(state.editingApp.appLabel) }
        AlertDialog(
            onDismissRequest = onCancelEditingApp,
            containerColor = colors.surface,
            shape = shapes.large,
            title = {
                Text("Edit App Label", style = typography.cardTitle, color = colors.textPrimary)
            },
            text = {
                OutlinedTextField(
                    value = newLabelText,
                    onValueChange = { newLabelText = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.borderSubtle,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { onSaveAppLabel(state.editingApp.packageName, newLabelText) },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text("Save", color = colors.background)
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelEditingApp) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun ApprovedAppItem(
    app: StudyApp,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = StudyTheme.colors
    val shapes = StudyTheme.shapes
    val typography = StudyTheme.typography

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appLabel,
                    style = typography.cardTitle,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = app.packageName,
                    style = typography.bodySmall,
                    color = colors.textTertiary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Label",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove App",
                        tint = colors.textTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Switch(
                    checked = app.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.background,
                        checkedTrackColor = colors.accent,
                        uncheckedThumbColor = colors.textSecondary,
                        uncheckedTrackColor = colors.surfaceElevated
                    )
                )
            }
        }
    }
}

@Composable
private fun AvailableAppItem(
    app: SelectableApp,
    onAdd: () -> Unit
) {
    val colors = StudyTheme.colors
    val shapes = StudyTheme.shapes
    val typography = StudyTheme.typography

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .background(colors.surfaceElevated.copy(alpha = 0.6f))
            .clickable(onClick = onAdd)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = app.packageName,
                    style = typography.bodySmall,
                    color = colors.textTertiary
                )
            }

            Box(
                modifier = Modifier
                    .clip(shapes.pill)
                    .background(colors.accentSubtle)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add",
                        style = typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.accent
                    )
                }
            }
        }
    }
}
