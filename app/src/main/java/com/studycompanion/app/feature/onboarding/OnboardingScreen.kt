package com.studycompanion.app.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studycompanion.app.domain.model.Profile
import com.studycompanion.app.domain.repository.AuthState

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onSignUp: (String, String) -> Unit,
    onLogin: (String, String) -> Unit,
    onCreateProfile: (String, String) -> Unit,
    onSetDailyTarget: (Long) -> Unit,
    onAdjustDailyTarget: (Long) -> Unit,
    onAddStudyApp: (String, String) -> Unit,
    onRemoveStudyApp: (String) -> Unit,
    onToggleStudyApp: (String, Boolean) -> Unit,
    onSwitchProfile: (String, String) -> Unit,
    onLockProfile: () -> Unit,
    onLogout: () -> Unit,
    onNavigateStep: (OnboardingStep) -> Unit,
    onLaunchPoc: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Chronoa",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )

            Text(
                text = "Phase 1: Architecture & Local Engine Scaffold",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Status feedback banner
            if (state.errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = state.errorMessage,
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (state.successMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14532D)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = state.successMessage,
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (state.isLoading) {
                CircularProgressIndicator(color = Color(0xFF38BDF8))
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Step Content
            when (state.currentStep) {
                OnboardingStep.AUTH -> {
                    AuthStepContent(
                        onSignUp = onSignUp,
                        onLogin = onLogin
                    )
                }
                OnboardingStep.PROFILE_CREATE -> {
                    ProfileCreateStepContent(
                        onCreateProfile = onCreateProfile,
                        canSkip = state.userProfiles.isNotEmpty(),
                        onSkip = { onNavigateStep(OnboardingStep.DASHBOARD) }
                    )
                }
                OnboardingStep.TARGET_SETUP -> {
                    TargetSetupStepContent(
                        onSetTarget = onSetDailyTarget,
                        onSkip = { onNavigateStep(OnboardingStep.STUDY_APPS_SETUP) }
                    )
                }
                OnboardingStep.STUDY_APPS_SETUP -> {
                    StudyAppsSetupStepContent(
                        availableApps = state.availableApps,
                        selectedApps = state.studyApps.map { it.packageName }.toSet(),
                        onAddApp = onAddStudyApp,
                        onRemoveApp = onRemoveStudyApp,
                        onDone = { onNavigateStep(OnboardingStep.DASHBOARD) }
                    )
                }
                OnboardingStep.DASHBOARD -> {
                    DashboardContent(
                        state = state,
                        onAdjustTarget = onAdjustDailyTarget,
                        onAddApp = onAddStudyApp,
                        onRemoveApp = onRemoveStudyApp,
                        onToggleApp = onToggleStudyApp,
                        onSwitchProfile = onSwitchProfile,
                        onLockProfile = onLockProfile,
                        onLogout = onLogout,
                        onNewProfile = { onNavigateStep(OnboardingStep.PROFILE_CREATE) },
                        onLaunchPoc = onLaunchPoc
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthStepContent(
    onSignUp: (String, String) -> Unit,
    onLogin: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Step 1: Account Authentication",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onSignUp(email, password) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                ) {
                    Text("Sign Up", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onLogin(email, password) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Log In", color = Color(0xFF38BDF8))
                }
            }
        }
    }
}

@Composable
private fun ProfileCreateStepContent(
    onCreateProfile: (String, String) -> Unit,
    canSkip: Boolean,
    onSkip: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Step 2: Create Profile & Secure PIN",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = "Profiles isolate study targets and sessions. PIN is hashed with PBKDF2.",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Profile Name (e.g. Shashank)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
                label = { Text("Profile PIN (4 to 6 numeric digits)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onCreateProfile(name, pin) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
            ) {
                Text("Create Profile", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            if (canSkip) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Skip to Dashboard", color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

@Composable
private fun TargetSetupStepContent(
    onSetTarget: (Long) -> Unit,
    onSkip: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Step 3: Set Daily Study Target",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = "Your daily study goal. Setting a target preserves originalTargetSeconds.",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onSetTarget(3600L) }, // 1 hr
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                ) {
                    Text("1h", color = Color.White)
                }
                Button(
                    onClick = { onSetTarget(7200L) }, // 2 hrs
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                ) {
                    Text("2h", color = Color.White)
                }
                Button(
                    onClick = { onSetTarget(10800L) }, // 3 hrs
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                ) {
                    Text("3h", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onSetTarget(14400L) }, // 4 hrs
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                ) {
                    Text("4h", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip for Now", color = Color(0xFF94A3B8))
            }
        }
    }
}

@Composable
private fun StudyAppsSetupStepContent(
    availableApps: List<com.studycompanion.app.domain.repository.SelectableApp>,
    selectedApps: Set<String>,
    onAddApp: (String, String) -> Unit,
    onRemoveApp: (String) -> Unit,
    onDone: () -> Unit
) {
    var customPackage by remember { mutableStateOf("") }
    var customLabel by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Step 4: Select Study Apps",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = "Select apps that count as study time. No hardcoded lists.",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            // Manual entry for testing or custom app
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customPackage,
                    onValueChange = { customPackage = it },
                    label = { Text("Package Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1.5f)
                )
                OutlinedTextField(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    label = { Text("Label") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (customPackage.isNotBlank()) {
                        onAddApp(customPackage.trim(), customLabel.trim().ifBlank { customPackage.trim() })
                        customPackage = ""
                        customLabel = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
            ) {
                Text("Add Custom App", color = Color.White)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Available Apps list
            Text(
                text = "Discovered Launchable Apps (${availableApps.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF38BDF8)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(vertical = 8.dp)
            ) {
                items(availableApps) { app ->
                    val isChecked = selectedApps.contains(app.packageName)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) onAddApp(app.packageName, app.label)
                                else onRemoveApp(app.packageName)
                            }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(text = app.label, color = Color.White, fontSize = 14.sp)
                            Text(text = app.packageName, color = Color(0xFF64748B), fontSize = 11.sp)
                        }
                    }
                }
            }

            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
            ) {
                Text("Complete Setup & View Dashboard", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DashboardContent(
    state: OnboardingUiState,
    onAdjustTarget: (Long) -> Unit,
    onAddApp: (String, String) -> Unit,
    onRemoveApp: (String) -> Unit,
    onToggleApp: (String, Boolean) -> Unit,
    onSwitchProfile: (String, String) -> Unit,
    onLockProfile: () -> Unit,
    onLogout: () -> Unit,
    onNewProfile: () -> Unit,
    onLaunchPoc: () -> Unit
) {
    var showSwitchDialog by remember { mutableStateOf(false) }
    var selectedProfileToSwitch by remember { mutableStateOf<Profile?>(null) }
    var switchPin by remember { mutableStateOf("") }

    var showAdjustTargetDialog by remember { mutableStateOf(false) }
    var adjustedMinutesInput by remember { mutableStateOf("120") }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Profile Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Active Profile",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = state.activeProfile?.name ?: "No active profile (Locked)",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showSwitchDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                            ) {
                                Text("Switch", color = Color.White, fontSize = 12.sp)
                            }
                            Button(
                                onClick = onLockProfile,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569))
                            ) {
                                Text("Lock", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Daily Target Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Today's Target",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val target = state.todayTarget
                    if (target != null) {
                        Text(
                            text = "Original Target: ${target.originalTargetSeconds / 60} mins (${target.originalTargetSeconds / 3600}h ${(target.originalTargetSeconds % 3600) / 60}m)",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        if (target.adjustedTargetSeconds != null) {
                            Text(
                                text = "Adjusted Target: ${target.adjustedTargetSeconds / 60} mins",
                                color = Color(0xFFFBBF24),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "Effective Target: ${target.effectiveTargetSeconds / 60} mins",
                            color = Color(0xFF34D399),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text("No target set for today", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showAdjustTargetDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                    ) {
                        Text("Adjust Today's Target (Preserves Original)", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }

        // Selected Study Apps Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Configured Study Apps (${state.studyApps.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (state.studyApps.isEmpty()) {
                        Text("No study apps added yet.", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    } else {
                        state.studyApps.forEach { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = app.appLabel, color = Color.White, fontSize = 14.sp)
                                    Text(text = app.packageName, color = Color(0xFF64748B), fontSize = 11.sp)
                                }
                                Switch(
                                    checked = app.isEnabled,
                                    onCheckedChange = { onToggleApp(app.packageName, it) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(onClick = { onRemoveApp(app.packageName) }) {
                                    Text("Remove", color = Color(0xFFF87171), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onNewProfile,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                ) {
                    Text("Create Another Profile", color = Color.White)
                }

                Button(
                    onClick = onLaunchPoc,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Launch Phase 0.2 POC Timeline", color = Color.White, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Logout", color = Color(0xFFF87171))
                }
            }
        }
    }

    // Switch Profile Dialog
    if (showSwitchDialog) {
        AlertDialog(
            onDismissRequest = { showSwitchDialog = false },
            title = { Text("Switch Profile") },
            text = {
                Column {
                    Text("Select a profile and enter its PIN:")
                    Spacer(modifier = Modifier.height(8.dp))
                    state.userProfiles.forEach { prof ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = selectedProfileToSwitch?.id == prof.id,
                                onCheckedChange = { if (it) selectedProfileToSwitch = prof }
                            )
                            Text(text = prof.name, color = Color.White, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = switchPin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) switchPin = it },
                        label = { Text("Enter 4-6 Digit PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetProf = selectedProfileToSwitch
                        if (targetProf != null && switchPin.isNotBlank()) {
                            onSwitchProfile(targetProf.id, switchPin)
                            showSwitchDialog = false
                            switchPin = ""
                        }
                    }
                ) {
                    Text("Verify & Switch")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSwitchDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Adjust Target Dialog
    if (showAdjustTargetDialog) {
        AlertDialog(
            onDismissRequest = { showAdjustTargetDialog = false },
            title = { Text("Adjust Today's Target") },
            text = {
                Column {
                    Text("Adjust target in minutes (original target remains preserved):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = adjustedMinutesInput,
                        onValueChange = { adjustedMinutesInput = it },
                        label = { Text("Minutes") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mins = adjustedMinutesInput.toLongOrNull()
                        if (mins != null && mins > 0) {
                            onAdjustTarget(mins * 60L)
                            showAdjustTargetDialog = false
                        }
                    }
                ) {
                    Text("Save Adjustment")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustTargetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
