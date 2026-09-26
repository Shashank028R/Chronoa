package com.studycompanion.app.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studycompanion.app.R
import com.studycompanion.app.core.ui.theme.StudyTheme
import com.studycompanion.app.domain.model.Profile
import com.studycompanion.app.domain.repository.SelectableApp

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
    val colors = StudyTheme.colors
    val shapes = StudyTheme.shapes
    val typography = StudyTheme.typography

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant Chronoa Brand Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 16.dp, bottom = 20.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_chronoa_logo),
                    contentDescription = "Chronoa Logo",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(shapes.medium)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Chronoa",
                        style = typography.profileName.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Smart Study Companion",
                        style = typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }

            // Status feedback banners
            if (state.errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.medium)
                        .background(colors.statusError.copy(alpha = 0.12f))
                        .border(1.dp, colors.statusError.copy(alpha = 0.35f), shapes.medium)
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = colors.statusError,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = state.errorMessage,
                            style = typography.bodySmall,
                            color = colors.textPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (state.successMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.medium)
                        .background(colors.statusStudying.copy(alpha = 0.12f))
                        .border(1.dp, colors.statusStudying.copy(alpha = 0.35f), shapes.medium)
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = colors.statusStudying,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = state.successMessage,
                            style = typography.bodySmall,
                            color = colors.textPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Step Content
            when (state.currentStep) {
                OnboardingStep.AUTH -> {
                    AuthStepContent(
                        isLoading = state.isLoading,
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
    isLoading: Boolean,
    onSignUp: (String, String) -> Unit,
    onLogin: (String, String) -> Unit
) {
    val colors = StudyTheme.colors
    val shapes = StudyTheme.shapes
    val typography = StudyTheme.typography
    val focusManager = LocalFocusManager.current

    var isSignUpMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var localValidationWarning by remember { mutableStateOf<String?>(null) }

    val isEmailValid = email.contains("@") && email.contains(".")
    val isPasswordValid = password.length >= 6
    val passwordsMatch = !isSignUpMode || (password == confirmPassword)

    val handleSubmit = {
        focusManager.clearFocus()
        when {
            email.isBlank() -> {
                localValidationWarning = "Please enter your email address."
            }
            !isEmailValid -> {
                localValidationWarning = "Please enter a valid email address."
            }
            password.isBlank() -> {
                localValidationWarning = "Please enter your password."
            }
            isSignUpMode && password.length < 6 -> {
                localValidationWarning = "Password must be at least 6 characters."
            }
            isSignUpMode && password != confirmPassword -> {
                localValidationWarning = "Passwords do not match."
            }
            else -> {
                localValidationWarning = null
                if (isSignUpMode) {
                    onSignUp(email.trim(), password)
                } else {
                    onLogin(email.trim(), password)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.large)
            .background(colors.surfaceElevated)
            .border(1.dp, colors.borderSubtle, shapes.large)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Segmented Tab Switcher (Log In vs Sign Up)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(shapes.pill)
                    .background(colors.surface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(shapes.pill)
                        .background(if (!isSignUpMode) colors.surfaceVariant else Color.Transparent)
                        .clickable {
                            isSignUpMode = false
                            localValidationWarning = null
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Log In",
                        style = typography.cardTitle.copy(
                            fontWeight = if (!isSignUpMode) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (!isSignUpMode) colors.textPrimary else colors.textSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(shapes.pill)
                        .background(if (isSignUpMode) colors.surfaceVariant else Color.Transparent)
                        .clickable {
                            isSignUpMode = true
                            localValidationWarning = null
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign Up",
                        style = typography.cardTitle.copy(
                            fontWeight = if (isSignUpMode) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSignUpMode) colors.textPrimary else colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (isSignUpMode) "Create an Account" else "Welcome Back",
                style = typography.cardTitle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )
            Text(
                text = if (isSignUpMode) "Start your automated study journey" else "Log in to access your study profiles",
                style = typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )

            // Local validation warning
            if (localValidationWarning != null) {
                Text(
                    text = localValidationWarning!!,
                    style = typography.bodySmall,
                    color = colors.statusError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            }

            // Email Input
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    localValidationWarning = null
                },
                label = { Text("Email Address") },
                placeholder = { Text("student@chronoa.app") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email",
                        tint = if (email.isNotBlank()) colors.accent else colors.textSecondary
                    )
                },
                trailingIcon = {
                    if (email.isNotBlank()) {
                        IconButton(onClick = { email = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = colors.textSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.borderSubtle,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedLabelColor = colors.accent,
                    unfocusedLabelColor = colors.textSecondary
                ),
                shape = shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Password Input
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    localValidationWarning = null
                },
                label = { Text("Password") },
                placeholder = { Text("At least 6 characters") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Password",
                        tint = if (password.isNotBlank()) colors.accent else colors.textSecondary
                    )
                },
                trailingIcon = {
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(
                            text = if (passwordVisible) "HIDE" else "SHOW",
                            style = typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.accent
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (isSignUpMode) ImeAction.Next else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { handleSubmit() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.borderSubtle,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedLabelColor = colors.accent,
                    unfocusedLabelColor = colors.textSecondary
                ),
                shape = shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            if (isSignUpMode) {
                Spacer(modifier = Modifier.height(14.dp))

                // Confirm Password Input
                val isMismatch = confirmPassword.isNotEmpty() && confirmPassword != password
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        localValidationWarning = null
                    },
                    label = { Text("Confirm Password") },
                    placeholder = { Text("Re-enter password") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Confirm Password",
                            tint = if (confirmPassword.isNotBlank() && !isMismatch) colors.accent else colors.textSecondary
                        )
                    },
                    trailingIcon = {
                        TextButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Text(
                                text = if (confirmPasswordVisible) "HIDE" else "SHOW",
                                style = typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.accent
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { handleSubmit() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface,
                        focusedBorderColor = if (isMismatch) colors.statusError else colors.accent,
                        unfocusedBorderColor = if (isMismatch) colors.statusError.copy(alpha = 0.5f) else colors.borderSubtle,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedLabelColor = colors.accent,
                        unfocusedLabelColor = colors.textSecondary
                    ),
                    shape = shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Action Button (Pill shaped, emerald)
            Button(
                onClick = handleSubmit,
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = shapes.pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.background,
                    disabledContainerColor = colors.accent.copy(alpha = 0.35f),
                    disabledContentColor = colors.background.copy(alpha = 0.6f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = colors.background,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = if (isSignUpMode) "Create Account" else "Log In",
                        style = typography.cardTitle.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Mode Toggle Link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.clickable {
                    isSignUpMode = !isSignUpMode
                    localValidationWarning = null
                }
            ) {
                Text(
                    text = if (isSignUpMode) "Already have an account? " else "Don't have an account? ",
                    style = typography.bodySmall,
                    color = colors.textSecondary
                )
                Text(
                    text = if (isSignUpMode) "Log In" else "Sign Up",
                    style = typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.accent
                )
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
    val colors = StudyTheme.colors
    val shapes = StudyTheme.shapes
    val typography = StudyTheme.typography

    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.large)
            .background(colors.surfaceElevated)
            .border(1.dp, colors.borderSubtle, shapes.large)
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "Create Profile & Secure PIN",
                style = typography.cardTitle.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )
            Text(
                text = "Profiles isolate study targets and sessions. PIN is hashed with PBKDF2.",
                style = typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Profile Name (e.g. Shashank)") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = colors.textSecondary)
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.borderSubtle,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                shape = shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
                label = { Text("Profile PIN (4 to 6 numeric digits)") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = colors.textSecondary)
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.borderSubtle,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                shape = shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onCreateProfile(name, pin) },
                enabled = name.isNotBlank() && pin.length >= 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = shapes.pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.background
                )
            ) {
                Text("Create Profile", style = typography.cardTitle.copy(fontWeight = FontWeight.Bold))
            }

            if (canSkip) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Skip to Dashboard", color = colors.textSecondary)
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
    val colors = StudyTheme.colors
    val shapes = StudyTheme.shapes
    val typography = StudyTheme.typography

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.large)
            .background(colors.surfaceElevated)
            .border(1.dp, colors.borderSubtle, shapes.large)
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "Set Daily Study Target",
                style = typography.cardTitle.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )
            Text(
                text = "Select your daily study goal. Setting a target preserves originalTargetSeconds.",
                style = typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onSetTarget(3600L) }, // 1 hr
                    modifier = Modifier.weight(1f),
                    shape = shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant)
                ) {
                    Text("1h", color = colors.textPrimary)
                }
                Button(
                    onClick = { onSetTarget(7200L) }, // 2 hrs
                    modifier = Modifier.weight(1f),
                    shape = shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant)
                ) {
                    Text("2h", color = colors.textPrimary)
                }
                Button(
                    onClick = { onSetTarget(10800L) }, // 3 hrs
                    modifier = Modifier.weight(1f),
                    shape = shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text("3h", color = colors.background, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onSetTarget(14400L) }, // 4 hrs
                    modifier = Modifier.weight(1f),
                    shape = shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant)
                ) {
                    Text("4h", color = colors.textPrimary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip for Now", color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun StudyAppsSetupStepContent(
    availableApps: List<SelectableApp>,
    selectedApps: Set<String>,
    onAddApp: (String, String) -> Unit,
    onRemoveApp: (String) -> Unit,
    onDone: () -> Unit
) {
    val colors = StudyTheme.colors
    val shapes = StudyTheme.shapes
    val typography = StudyTheme.typography

    var customPackage by remember { mutableStateOf("") }
    var customLabel by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.large)
            .background(colors.surfaceElevated)
            .border(1.dp, colors.borderSubtle, shapes.large)
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "Select Approved Study Apps",
                style = typography.cardTitle.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )
            Text(
                text = "Select apps that count towards your study focus.",
                style = typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            // Available Apps list
            Text(
                text = "Available Apps (${availableApps.size})",
                style = typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = colors.accent
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .padding(vertical = 8.dp)
            ) {
                items(availableApps) { app ->
                    val isChecked = selectedApps.contains(app.packageName)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) onRemoveApp(app.packageName)
                                else onAddApp(app.packageName, app.label)
                            }
                            .padding(vertical = 6.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) onAddApp(app.packageName, app.label)
                                else onRemoveApp(app.packageName)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = colors.accent,
                                uncheckedColor = colors.textSecondary
                            )
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(text = app.label, color = colors.textPrimary, style = typography.cardTitle)
                            Text(text = app.packageName, color = colors.textSecondary, style = typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = shapes.pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.background
                )
            ) {
                Text("Complete Setup", style = typography.cardTitle.copy(fontWeight = FontWeight.Bold))
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
    val colors = StudyTheme.colors
    val shapes = StudyTheme.shapes
    val typography = StudyTheme.typography

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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.large)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.borderSubtle, shapes.large)
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Active Profile",
                                style = typography.bodySmall,
                                color = colors.textSecondary
                            )
                            Text(
                                text = state.activeProfile?.name ?: "No active profile (Locked)",
                                style = typography.profileName,
                                color = colors.textPrimary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showSwitchDialog = true },
                                shape = shapes.pill,
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                            ) {
                                Text("Switch", color = colors.background)
                            }
                            Button(
                                onClick = onLockProfile,
                                shape = shapes.pill,
                                colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant)
                            ) {
                                Text("Lock", color = colors.textPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Profiles: ${state.userProfiles.joinToString { it.name }}",
                        style = typography.bodySmall,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onNewProfile,
                            shape = shapes.pill,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant)
                        ) {
                            Text("+ New Profile", color = colors.textPrimary)
                        }
                        Button(
                            onClick = onLogout,
                            shape = shapes.pill,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant)
                        ) {
                            Text("Logout", color = colors.statusError)
                        }
                    }
                }
            }
        }

        // Daily Target Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.large)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.borderSubtle, shapes.large)
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Daily Target",
                                style = typography.bodySmall,
                                color = colors.textSecondary
                            )
                            Text(
                                text = state.todayTarget?.let { "${it.effectiveTargetSeconds / 60}m (${it.effectiveTargetSeconds / 3600}h)" }
                                    ?: "No target set",
                                style = typography.profileName,
                                color = colors.textPrimary
                            )
                        }
                        Button(
                            onClick = { showAdjustTargetDialog = true },
                            shape = shapes.pill,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant)
                        ) {
                            Text("Adjust Target", color = colors.textPrimary)
                        }
                    }
                }
            }
        }
    }

    // Switch Profile Dialog
    if (showSwitchDialog) {
        AlertDialog(
            onDismissRequest = { showSwitchDialog = false },
            title = { Text("Switch Profile", color = colors.textPrimary) },
            text = {
                Column {
                    Text("Select a profile and enter its PIN:", color = colors.textSecondary)
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
                                onCheckedChange = { if (it) selectedProfileToSwitch = prof },
                                colors = CheckboxDefaults.colors(checkedColor = colors.accent)
                            )
                            Text(text = prof.name, color = colors.textPrimary, modifier = Modifier.padding(start = 8.dp))
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
                    },
                    shape = shapes.pill,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.background)
                ) {
                    Text("Verify & Switch")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSwitchDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Adjust Target Dialog
    if (showAdjustTargetDialog) {
        AlertDialog(
            onDismissRequest = { showAdjustTargetDialog = false },
            title = { Text("Adjust Today's Target", color = colors.textPrimary) },
            text = {
                Column {
                    Text("Adjust target in minutes:", color = colors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = adjustedMinutesInput,
                        onValueChange = { adjustedMinutesInput = it },
                        label = { Text("Minutes") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    },
                    shape = shapes.pill,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.background)
                ) {
                    Text("Save Adjustment")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustTargetDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}
