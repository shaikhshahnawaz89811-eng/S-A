package com.sacompanion.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sacompanion.ui.theme.*
import com.sacompanion.ui.viewmodel.MainViewModel
import com.sacompanion.ui.viewmodel.ChatMessage
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val isProcessing = uiState.currentServiceState == "PROCESSING"

    // Auto-scroll to bottom
    LaunchedEffect(uiState.chatMessages.size) {
        if (uiState.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.chatMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkBase, Color(0xFF050E1F), DarkBase)))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextSecondary)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text("SA CHAT", fontSize = 14.sp, color = TextPrimary, letterSpacing = 3.sp)
                Text("AI Conversation", fontSize = 10.sp, color = TextSecondary)
            }
            Spacer(Modifier.weight(1f))
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = CyberBlue,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
            }
            IconButton(onClick = { viewModel.clearConversation() }) {
                Icon(Icons.Default.DeleteOutline, "Clear", tint = TextSecondary)
            }
        }

        HorizontalDivider(color = GlassBorder, thickness = 1.dp)

        // Messages
        if (uiState.chatMessages.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SA", fontSize = 48.sp, color = CyberBlue.copy(alpha = 0.3f), letterSpacing = 8.sp)
                    Text("Namaste! Main SA hoon.", color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
                    Text("Kuch bhi poochho — main yahan hoon.", color = TextSecondary.copy(alpha = 0.6f), fontSize = 12.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    // Quick prompts
                    listOf(
                        "Battery kitni hai?",
                        "Abhi time kya hai?",
                        "Torch on karo",
                        "SA, volume badha do"
                    ).forEach { prompt ->
                        OutlinedButton(
                            onClick = { viewModel.sendTextMessage(prompt); inputText = "" },
                            border = BorderStroke(1.dp, CyberBlue.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(prompt, color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(uiState.chatMessages) { message ->
                    ChatBubble(message = message)
                }
                if (isProcessing) {
                    item {
                        Row(modifier = Modifier.padding(start = 8.dp)) {
                            TypingIndicator()
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = GlassBorder, thickness = 1.dp)

        // Input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Kuch bhi poochho...", color = TextSecondary.copy(0.5f), fontSize = 14.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberBlue,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = CyberBlue
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendTextMessage(inputText)
                        inputText = ""
                        scope.launch {
                            if (uiState.chatMessages.isNotEmpty()) {
                                listState.animateScrollToItem(uiState.chatMessages.size - 1)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (inputText.isNotBlank()) CyberBlue else GlassWhite,
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    "Send",
                    tint = if (inputText.isNotBlank()) DarkBase else TextSecondary
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            Box(
                modifier = Modifier.size(28.dp).background(CyberBlue.copy(0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("SA", fontSize = 8.sp, color = CyberBlue)
            }
            Spacer(Modifier.width(6.dp))
        }
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    if (message.isUser) PlasmaViolet.copy(0.2f) else GlassWhite,
                    RoundedCornerShape(
                        topStart = if (message.isUser) 16.dp else 4.dp,
                        topEnd = if (message.isUser) 4.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    )
                )
                .border(
                    1.dp,
                    if (message.isUser) PlasmaViolet.copy(0.3f) else GlassBorder,
                    RoundedCornerShape(
                        topStart = if (message.isUser) 16.dp else 4.dp,
                        topEnd = if (message.isUser) 4.dp else 16.dp,
                        bottomStart = 16.dp, bottomEnd = 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            Text(message.text, color = TextPrimary, fontSize = 14.sp)
        }
        if (message.isUser) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier.size(28.dp).background(PlasmaViolet.copy(0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = PlasmaViolet, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dot1 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "d1")
    val dot2 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600, delayMillis = 150), RepeatMode.Reverse), label = "d2")
    val dot3 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600, delayMillis = 300), RepeatMode.Reverse), label = "d3")
    Row(
        modifier = Modifier
            .background(GlassWhite, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(dot1, dot2, dot3).forEach { alpha ->
            Box(Modifier.size(6.dp).background(CyberBlue.copy(alpha = alpha), CircleShape))
        }
    }
}
