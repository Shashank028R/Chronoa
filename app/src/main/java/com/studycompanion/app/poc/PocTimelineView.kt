package com.studycompanion.app.poc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PocTimelineView(
    state: PocEngineState,
    onOpenUsageSettings: () -> Unit,
    onAddApprovedPackage: (String) -> Unit,
    onRemoveApprovedPackage: (String) -> Unit
) {
    var newPackageInput by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column {
                    Text(
                        text = "UsageEvents Proof of Concept",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Phase 0.2 Technical Feasibility Experiment",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }

            // 1. Permission Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.isUsageAccessGranted) Color(0xFF1B3320) else Color(0xFF3B1E1E)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Usage Access Permission",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (state.isUsageAccessGranted) Color(0xFF2E7D32) else Color(0xFFC62828),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (state.isUsageAccessGranted) "GRANTED" else "NOT GRANTED",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (!state.isUsageAccessGranted) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Android requires special Usage Access to query UsageEvents. Tap below to enable in system settings.",
                                color = Color(0xFFFFCDD2),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onOpenUsageSettings,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Open Usage Access Settings", color = Color.White)
                            }
                        }
                    }
                }
            }

            // 2. Current State Display
            item {
                val stateColor = when (state.currentFocusState) {
                    PocFocusState.FOCUSING -> Color(0xFF2E7D32)
                    PocFocusState.PAUSED_UNAPPROVED -> Color(0xFFE65100)
                    PocFocusState.PAUSED_HOME -> Color(0xFF1565C0)
                    PocFocusState.UNKNOWN -> Color(0xFF616161)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "CURRENT ENGINE STATE",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .background(color = stateColor, shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = state.currentFocusState.displayTitle,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Foreground Package:",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                        Text(
                            text = state.currentForegroundPackage ?: "None / Waiting for events",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Delay: ${state.lastEventDelayMillis} ms",
                                color = Color(0xFF81D4FA),
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Processed: ${state.totalEventsProcessed}",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // 3. Approved Packages Manager
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "APPROVED STUDY PACKAGES",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            state.approvedPackages.forEach { pkg ->
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF2C2C2C), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = pkg,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        OutlinedButton(
                                            onClick = { onRemoveApprovedPackage(pkg) },
                                            modifier = Modifier.height(24.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                                        ) {
                                            Text("✕", fontSize = 10.sp, color = Color.Red)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newPackageInput,
                                onValueChange = { newPackageInput = it },
                                label = { Text("Add package (e.g. com.android.chrome)", color = Color.Gray, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newPackageInput.isNotBlank()) {
                                        onAddApprovedPackage(newPackageInput.trim())
                                        newPackageInput = ""
                                    }
                                }
                            ) {
                                Text("Add")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        // Quick Suggestions
                        Text("Quick Add Suggestions:", color = Color.Gray, fontSize = 11.sp)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            listOf("com.android.chrome", "com.google.android.youtube", "com.android.settings").forEach { suggestion ->
                                if (!state.approvedPackages.contains(suggestion)) {
                                    OutlinedButton(
                                        onClick = { onAddApprovedPackage(suggestion) },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(suggestion.substringAfterLast("."), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Detected Launcher Packages
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "DETECTED LAUNCHER (HOME SCREEN)",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (state.launcherPackages.isNotEmpty()) {
                                state.launcherPackages.joinToString(", ")
                            } else {
                                "Resolving launcher..."
                            },
                            color = Color(0xFF90CAF9),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // 5. Timeline Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent UsageEvents Timeline",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${state.recentTimeline.size} events",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            // 6. Timeline Events
            if (state.recentTimeline.isEmpty()) {
                item {
                    Text(
                        text = if (!state.isUsageAccessGranted) {
                            "Grant Usage Access to observe events."
                        } else {
                            "Polling active. Open other apps to observe transitions."
                        },
                        color = Color.Gray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(state.recentTimeline, key = { it.id }) { event ->
                    TimelineEventRow(event)
                }
            }
        }
    }
}

@Composable
fun TimelineEventRow(event: PocUsageEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF242424)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = event.formattedEventTime,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = event.eventType,
                    color = when (event.eventType) {
                        "ACTIVITY_RESUMED" -> Color(0xFF81C784)
                        "ACTIVITY_PAUSED" -> Color(0xFFFFB74D)
                        "KEYGUARD_SHOWN", "KEYGUARD_HIDDEN" -> Color(0xFF64B5F6)
                        else -> Color.LightGray
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = event.packageName,
                color = Color.LightGray,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "State: ${event.resultingState.displayTitle}",
                    color = when (event.resultingState) {
                        PocFocusState.FOCUSING -> Color(0xFF81C784)
                        PocFocusState.PAUSED_HOME -> Color(0xFF90CAF9)
                        PocFocusState.PAUSED_UNAPPROVED -> Color(0xFFFFB74D)
                        PocFocusState.UNKNOWN -> Color.Gray
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "delay +${event.delayMillis}ms",
                    color = Color.DarkGray,
                    fontSize = 10.sp
                )
            }
        }
    }
}
