package com.studycompanion.app.feature.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.studycompanion.app.core.ui.theme.StudyTheme
import com.studycompanion.app.domain.model.Profile

@Composable
fun ProfileSwitchDialog(
    profiles: List<Profile>,
    activeProfileId: String?,
    onDismiss: () -> Unit,
    onSwitchProfile: (profileId: String, pin: String) -> Unit,
    onCreateProfile: (name: String, pin: String) -> Unit
) {
    val colors = StudyTheme.colors
    val shapes = StudyTheme.shapes
    val typography = StudyTheme.typography

    var selectedProfileForPin by remember { mutableStateOf<Profile?>(null) }
    var pinInput by remember { mutableStateOf("") }
    var showCreateProfile by remember { mutableStateOf(false) }

    var newProfileName by remember { mutableStateOf("") }
    var newProfilePin by remember { mutableStateOf("") }

    if (showCreateProfile) {
        AlertDialog(
            onDismissRequest = { showCreateProfile = false },
            containerColor = colors.surface,
            shape = shapes.large,
            title = {
                Text("Create New Profile", style = typography.screenTitle, color = colors.textPrimary)
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        label = { Text("Profile Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.borderSubtle
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newProfilePin,
                        onValueChange = { if (it.length <= 6) newProfilePin = it },
                        label = { Text("Security PIN (4–6 digits)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.borderSubtle
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProfileName.isNotBlank() && newProfilePin.length >= 4) {
                            onCreateProfile(newProfileName.trim(), newProfilePin)
                            showCreateProfile = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text("Create", color = colors.background)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateProfile = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    if (selectedProfileForPin != null) {
        AlertDialog(
            onDismissRequest = {
                selectedProfileForPin = null
                pinInput = ""
            },
            containerColor = colors.surface,
            shape = shapes.large,
            title = {
                Text("Enter PIN", style = typography.screenTitle, color = colors.textPrimary)
            },
            text = {
                Column {
                    Text(
                        text = "Enter PIN to switch to '${selectedProfileForPin?.name}'",
                        style = typography.bodyMedium,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 6) pinInput = it },
                        label = { Text("PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.borderSubtle
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val prof = selectedProfileForPin ?: return@Button
                        onSwitchProfile(prof.id, pinInput)
                        selectedProfileForPin = null
                        pinInput = ""
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text("Switch", color = colors.background)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedProfileForPin = null
                    pinInput = ""
                }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = shapes.large,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Switch Profile", style = typography.screenTitle, color = colors.textPrimary)
                IconButton(onClick = { showCreateProfile = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Profile",
                        tint = colors.accent
                    )
                }
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(profiles, key = { it.id }) { profile ->
                    val isActive = profile.id == activeProfileId
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.medium)
                            .background(if (isActive) colors.accentSubtle else colors.surfaceElevated)
                            .clickable {
                                if (!isActive) {
                                    selectedProfileForPin = profile
                                }
                            }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (isActive) colors.accent else colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = profile.name,
                                        style = typography.cardTitle,
                                        color = colors.textPrimary
                                    )
                                    if (isActive) {
                                        Text(
                                            text = "Active Profile",
                                            style = typography.bodySmall,
                                            color = colors.accent
                                        )
                                    }
                                }
                            }

                            if (isActive) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Active",
                                    tint = colors.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Requires PIN",
                                    tint = colors.textTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = colors.textPrimary)
            }
        }
    )
}
