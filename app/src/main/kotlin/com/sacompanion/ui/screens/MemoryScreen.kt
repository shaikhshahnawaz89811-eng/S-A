package com.sacompanion.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sacompanion.SAApplication
import com.sacompanion.core.memory.MemoryManager
import com.sacompanion.database.entities.MemoryEntity
import com.sacompanion.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun MemoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as SAApplication
    val memoryManager = app.memoryManager
    val memories by memoryManager.getAllMemoriesFlow().collectAsState(emptyList())
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var newKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("general") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkBase, Color(0xFF050E1F), DarkBase)))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextSecondary)
            }
            Column {
                Text("MEMORY", fontSize = 14.sp, color = TextPrimary, letterSpacing = 3.sp)
                Text("${memories.size} stored facts", fontSize = 10.sp, color = TextSecondary)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add Memory", tint = CyberBlue)
            }
        }

        HorizontalDivider(color = GlassBorder, thickness = 1.dp)

        if (memories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Memory, null, tint = TextSecondary.copy(0.3f), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No memories yet", color = TextSecondary, fontSize = 16.sp)
                    Text("Tap + to add facts SA should remember", color = TextSecondary.copy(0.6f), fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(memories, key = { it.id }) { memory ->
                    MemoryCard(memory = memory, onDelete = {
                        scope.launch { memoryManager.deleteMemory(memory.key) }
                    })
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = DarkSurface,
            title = { Text("Add Memory", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it },
                        label = { Text("Key (e.g. owner_name)", color = TextSecondary) },
                        colors = memoryFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        label = { Text("Value", color = TextSecondary) },
                        colors = memoryFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = { newCategory = it },
                        label = { Text("Category (general/personal/security)", color = TextSecondary) },
                        colors = memoryFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newKey.isNotBlank() && newValue.isNotBlank()) {
                        memoryManager.saveMemory(newKey.trim(), newValue.trim(), newCategory.trim().ifEmpty { "general" })
                        newKey = ""; newValue = ""; newCategory = "general"
                        showAddDialog = false
                    }
                }) { Text("Save", color = CyberBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }
}

@Composable
fun MemoryCard(memory: MemoryEntity, onDelete: () -> Unit) {
    val categoryColor = when (memory.category) {
        "security" -> HUDOrange
        "personal" -> PlasmaViolet
        else -> CyberBlue
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlassWhite, RoundedCornerShape(10.dp))
            .border(1.dp, categoryColor.copy(0.2f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(6.dp).offset(y = 6.dp).background(categoryColor, androidx.compose.foundation.shape.CircleShape)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(memory.key, color = categoryColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(2.dp))
            Text(memory.value, color = TextPrimary, fontSize = 13.sp)
            Text(memory.category, color = TextSecondary, fontSize = 9.sp)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, "Delete", tint = HUDOrange.copy(0.6f), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun memoryFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyberBlue,
    unfocusedBorderColor = GlassBorder,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)
