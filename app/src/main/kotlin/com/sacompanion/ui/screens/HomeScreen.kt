package com.sacompanion.ui.screens

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sacompanion.service.background.FloatingWindowService
import com.sacompanion.ui.theme.*
import com.sacompanion.ui.viewmodel.MainViewModel
import kotlin.math.*

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToChat: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onNavigateToFamily: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onStartService: () -> Unit,
    onRequestOverlay: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "home")
    val orbPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "orb"
    )
    val ringAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "ring"
    )
    val outerRingAngle by infiniteTransition.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "ring2"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1500, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(DarkBase, Color(0xFF050E1F), DarkBase))
            )
    ) {
        // Background grid lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSpacing = 60.dp.toPx()
            val gridColor = CyberBlue.copy(alpha = 0.04f)
            var x = 0f
            while (x < size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1f)
                x += gridSpacing
            }
            var y = 0f
            while (y < size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f)
                y += gridSpacing
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Status bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SA COMPANION", fontSize = 10.sp, color = CyberBlue.copy(alpha = 0.7f),
                    letterSpacing = 3.sp, fontWeight = FontWeight.Light)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(6.dp).background(if (uiState.isServiceRunning) NeonGreen else HUDOrange, CircleShape))
                    Text(
                        if (uiState.isServiceRunning) "ONLINE" else "STANDBY",
                        fontSize = 9.sp, color = if (uiState.isServiceRunning) NeonGreen else HUDOrange,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // SA Core status
            Text(
                "SA CORE ONLINE",
                fontSize = 13.sp,
                color = CyberBlue.copy(alpha = 0.9f),
                letterSpacing = 4.sp,
                fontWeight = FontWeight.Light
            )

            Spacer(Modifier.height(32.dp))

            // AI ORB
            Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                // Outer glow
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(CyberBlue.copy(alpha = glowAlpha * 0.3f), Color.Transparent),
                            center = center, radius = size.minDimension / 1.5f
                        ),
                        radius = size.minDimension / 1.5f, center = center
                    )

                    // Outer rotating ring
                    val outerR = size.minDimension / 2 - 8f
                    for (i in 0..3) {
                        val angle = Math.toRadians(outerRingAngle + i * 90.0)
                        val dotX = center.x + outerR * cos(angle).toFloat()
                        val dotY = center.y + outerR * sin(angle).toFloat()
                        drawCircle(CyberBlue.copy(alpha = 0.6f), 4f, Offset(dotX, dotY))
                    }
                    drawCircle(CyberBlue.copy(alpha = 0.15f), outerR, center, style = Stroke(1f))

                    // Inner rotating ring
                    val innerR = size.minDimension / 2.6f
                    for (i in 0..5) {
                        val angle = Math.toRadians(ringAngle + i * 60.0)
                        val dotX = center.x + innerR * cos(angle).toFloat()
                        val dotY = center.y + innerR * sin(angle).toFloat()
                        drawCircle(NeonGreen.copy(alpha = 0.5f), 3f, Offset(dotX, dotY))
                    }
                }

                // Core orb
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(orbPulse)
                        .background(
                            brush = Brush.radialGradient(
                                listOf(
                                    CyberBlue.copy(alpha = 0.25f),
                                    DarkCard.copy(alpha = 0.9f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(1.dp, CyberBlue.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SA", fontSize = 40.sp, fontWeight = FontWeight.Thin, color = CyberBlue, letterSpacing = 4.sp)
                        Text(
                            when (uiState.currentServiceState) {
                                "PROCESSING" -> "THINKING"
                                "LISTENING" -> "LISTENING"
                                "RESPONSE" -> "SPEAKING"
                                else -> "READY"
                            },
                            fontSize = 8.sp, color = NeonGreen.copy(alpha = 0.8f), letterSpacing = 2.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Last response
            if (uiState.lastResponse.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .background(GlassWhite, RoundedCornerShape(12.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        uiState.lastResponse,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.height(8.dp))

            // Main action buttons grid
            val buttons = listOf(
                Triple("Voice", Icons.Default.Mic, CyberBlue),
                Triple("AI Chat", Icons.Default.Chat, PlasmaViolet),
                Triple("Phone", Icons.Default.PhoneAndroid, NeonGreen),
                Triple("Music", Icons.Default.MusicNote, HUDOrange),
                Triple("Memory", Icons.Default.Memory, CyberBlue),
                Triple("Settings", Icons.Default.Settings, TextSecondary)
            )

            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                buttons.chunked(3).forEach { rowButtons ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowButtons.forEach { (label, icon, color) ->
                            HUDButton(
                                label = label,
                                icon = icon,
                                accentColor = color,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    when (label) {
                                        "Voice" -> {
                                            if (!uiState.isServiceRunning) {
                                                onStartService()
                                                viewModel.setServiceRunning(true)
                                            }
                                        }
                                        "AI Chat" -> onNavigateToChat()
                                        "Memory" -> onNavigateToMemory()
                                        "Settings" -> onNavigateToSettings()
                                        "Phone" -> {
                                            // Quick phone actions handled in chat
                                            onNavigateToChat()
                                        }
                                        "Music" -> {
                                            // Open music intent
                                            val intent = context.packageManager
                                                .getLaunchIntentForPackage("com.spotify.music")
                                                ?: context.packageManager
                                                    .getLaunchIntentForPackage("com.google.android.music")
                                            intent?.let {
                                                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                context.startActivity(it)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Service toggle
            Button(
                onClick = {
                    if (uiState.isServiceRunning) {
                        val stopIntent = Intent(context, com.sacompanion.service.background.SAAssistantService::class.java)
                        context.stopService(stopIntent)
                        viewModel.setServiceRunning(false)
                    } else {
                        onStartService()
                        viewModel.setServiceRunning(true)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isServiceRunning) HUDOrange.copy(alpha = 0.15f) else NeonGreen.copy(alpha = 0.15f)
                ),
                border = BorderStroke(1.dp, if (uiState.isServiceRunning) HUDOrange else NeonGreen),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(horizontal = 48.dp).fillMaxWidth()
            ) {
                Icon(
                    if (uiState.isServiceRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (uiState.isServiceRunning) HUDOrange else NeonGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (uiState.isServiceRunning) "STOP ASSISTANT" else "START ASSISTANT",
                    color = if (uiState.isServiceRunning) HUDOrange else NeonGreen,
                    fontSize = 11.sp, letterSpacing = 2.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // Floating window button
            TextButton(onClick = {
                onRequestOverlay()
                val intent = Intent(context, FloatingWindowService::class.java)
                context.startService(intent)
            }) {
                Icon(Icons.Default.Layers, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Floating Window", color = TextSecondary, fontSize = 11.sp)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun HUDButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = GlassWhite)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = accentColor, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, color = TextPrimary, fontSize = 10.sp, letterSpacing = 0.5.sp, textAlign = TextAlign.Center)
        }
    }
}
