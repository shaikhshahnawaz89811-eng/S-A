package com.sacompanion.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sacompanion.ui.theme.*
import com.sacompanion.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var apiKeyText by remember { mutableStateOf(uiState.apiKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var showSaveConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkBase, Color(0xFF050E1F), DarkBase)))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextSecondary) }
            Column {
                Text("SETTINGS", fontSize = 14.sp, color = TextPrimary, letterSpacing = 3.sp)
                Text("SA Companion Configuration", fontSize = 10.sp, color = TextSecondary)
            }
        }
        HorizontalDivider(color = GlassBorder, thickness = 1.dp)

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AI Configuration
            SettingsSectionHeader("AI CONFIGURATION", Icons.Default.Psychology)

            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Groq API Key", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "Get your free API key from console.groq.com",
                        color = TextSecondary, fontSize = 11.sp
                    )
                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = { apiKeyText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("gsk_...", color = TextSecondary.copy(0.4f)) },
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberBlue,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null, tint = TextSecondary
                                )
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    Button(
                        onClick = {
                            viewModel.saveApiKey(apiKeyText.trim())
                            showSaveConfirm = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberBlue.copy(0.2f)),
                        border = BorderStroke(1.dp, CyberBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Save, null, tint = CyberBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save API Key", color = CyberBlue)
                    }
                    if (showSaveConfirm) {
                        Text("✓ API key saved securely", color = NeonGreen, fontSize = 12.sp)
                    }
                }
            }

            // Voice Settings
            SettingsSectionHeader("VOICE SETTINGS", Icons.Default.RecordVoiceOver)
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsInfoRow("Language", "Hindi / Hinglish / English")
                    SettingsInfoRow("Wake Word", "\"SA\"")
                    SettingsInfoRow("TTS Engine", "Android TTS (Female voice)")
                    SettingsInfoRow("Speech Rate", "0.9x (Natural)")
                }
            }

            // About
            SettingsSectionHeader("ABOUT", Icons.Default.Info)
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsInfoRow("App Name", "SA Companion")
                    SettingsInfoRow("Version", "1.0.0")
                    SettingsInfoRow("AI Model", "Llama 3.3 70B (Groq)")
                    SettingsInfoRow("Architecture", "Kotlin + Jetpack Compose")
                    SettingsInfoRow("Min Android", "Android 8.0 (API 26)")
                }
            }

            // Danger Zone
            SettingsSectionHeader("DATA MANAGEMENT", Icons.Default.Warning)
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.clearConversation() },
                        border = BorderStroke(1.dp, HUDOrange.copy(0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteSweep, null, tint = HUDOrange, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Clear Conversation History", color = HUDOrange)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = CyberBlue, modifier = Modifier.size(14.dp))
        Text(title, color = CyberBlue, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
        Box(Modifier.weight(1f).height(1.dp).background(CyberBlue.copy(0.2f)))
    }
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlassWhite, RoundedCornerShape(12.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) { content() }
}

@Composable
fun SettingsInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(value, color = TextPrimary, fontSize = 12.sp)
    }
}
