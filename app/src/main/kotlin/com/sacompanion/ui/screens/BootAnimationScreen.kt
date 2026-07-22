package com.sacompanion.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sacompanion.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun BootAnimationScreen(onBootComplete: () -> Unit) {
    var phase by remember { mutableIntStateOf(0) }

    val infiniteTransition = rememberInfiniteTransition(label = "boot")
    val orbPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "orb_pulse"
    )
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "ring_rotation"
    )

    val phaseAlpha by animateFloatAsState(
        targetValue = if (phase >= 1) 1f else 0f,
        animationSpec = tween(800), label = "phase_alpha"
    )

    val bootLines = listOf(
        "INITIALIZING SA CORE...",
        "LOADING AI BRAIN...",
        "VOICE ENGINE READY",
        "MEMORY SYSTEMS ONLINE",
        "TTS INITIALIZED",
        "SA COMPANION v1.0 READY"
    )
    var visibleLines by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        delay(400)
        phase = 1
        repeat(bootLines.size) {
            delay(350)
            visibleLines = it + 1
        }
        delay(800)
        onBootComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(DarkSurface, DarkBase),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.alpha(phaseAlpha)
        ) {
            // SA Logo / Orb
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(orbPulse),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                    val radius = size.minDimension / 2.2f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(CyberBlue.copy(alpha = 0.3f), CyberBlue.copy(alpha = 0f)),
                            center = center, radius = radius * 1.5f
                        ),
                        radius = radius * 1.5f, center = center
                    )
                    drawCircle(
                        color = CyberBlue.copy(alpha = 0.8f),
                        radius = radius, center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )
                }
                Text(
                    text = "SA",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberBlue,
                    letterSpacing = 4.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "SA COMPANION",
                fontSize = 22.sp,
                fontWeight = FontWeight.Light,
                color = TextPrimary,
                letterSpacing = 6.sp
            )
            Text(
                text = "PERSONAL AI ASSISTANT",
                fontSize = 11.sp,
                color = TextSecondary,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Boot log
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.width(280.dp)
            ) {
                bootLines.take(visibleLines).forEach { line ->
                    Text(
                        text = "> $line",
                        fontSize = 11.sp,
                        color = if (line.contains("READY") || line.contains("ONLINE"))
                            NeonGreen else CyberBlue.copy(alpha = 0.8f),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}
