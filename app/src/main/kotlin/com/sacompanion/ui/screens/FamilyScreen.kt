package com.sacompanion.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.sacompanion.SAApplication
import com.sacompanion.database.entities.FamilyProfileEntity
import com.sacompanion.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun FamilyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as SAApplication
    val memoryManager = app.memoryManager
    val profiles by memoryManager.getAllFamilyProfilesFlow().collectAsState(emptyList())
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<FamilyProfileEntity?>(null) }
    var name by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("") }
    var accessLevel by remember { mutableStateOf("limited") }
    var notes by remember { mutableStateOf("") }
    var isOwner by remember { mutableStateOf(false) }

    fun resetForm() { name = ""; relation = ""; accessLevel = "limited"; notes = ""; isOwner = false; editingProfile = null }

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
                Text("FAMILY PROFILES", fontSize = 14.sp, color = TextPrimary, letterSpacing = 3.sp)
                Text("${profiles.size} members", fontSize = 10.sp, color = TextSecondary)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.PersonAdd, "Add", tint = CyberBlue)
            }
        }
        HorizontalDivider(color = GlassBorder, thickness = 1.dp)

        // Info box
        Box(modifier = Modifier.fillMaxWidth().padding(12.dp)
            .background(NeonGreen.copy(0.05f), RoundedCornerShape(8.dp))
            .border(1.dp, NeonGreen.copy(0.2f), RoundedCornerShape(8.dp))
            .padding(12.dp)
        ) {
            Text(
                "Family profiles control who can access what.\nOwner has full access. Family gets limited access. Guests get basic responses only.",
                color = NeonGreen.copy(0.8f), fontSize = 11.sp, lineHeight = 16.sp
            )
        }

        if (profiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FamilyRestroom, null, tint = TextSecondary.copy(0.3f), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No family profiles", color = TextSecondary, fontSize = 16.sp)
                    Text("Add your family members", color = TextSecondary.copy(0.6f), fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(profiles, key = { it.id }) { profile ->
                    FamilyProfileCard(
                        profile = profile,
                        onEdit = {
                            editingProfile = profile
                            name = profile.name; relation = profile.relation
                            accessLevel = profile.accessLevel; notes = profile.notes
                            isOwner = profile.isOwner; showAddDialog = true
                        },
                        onDelete = { scope.launch { memoryManager.deleteFamilyProfile(profile.id) } }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false; resetForm() },
            containerColor = DarkSurface,
            title = { Text(if (editingProfile != null) "Edit Profile" else "Add Family Member", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it },
                        label = { Text("Name", color = TextSecondary) }, colors = memoryFieldColors(), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = relation, onValueChange = { relation = it },
                        label = { Text("Relation (owner/family/guest)", color = TextSecondary) }, colors = memoryFieldColors(), modifier = Modifier.fillMaxWidth())
                    // Access Level
                    Text("Access Level", color = TextSecondary, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("full", "limited", "basic").forEach { level ->
                            FilterChip(selected = accessLevel == level, onClick = { accessLevel = level },
                                label = { Text(level.replaceFirstChar { it.uppercaseChar() }, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberBlue.copy(0.2f),
                                    selectedLabelColor = CyberBlue
                                )
                            )
                        }
                    }
                    OutlinedTextField(value = notes, onValueChange = { notes = it },
                        label = { Text("Notes (optional)", color = TextSecondary) }, colors = memoryFieldColors(),
                        modifier = Modifier.fillMaxWidth(), maxLines = 3)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isOwner, onCheckedChange = { isOwner = it },
                            colors = CheckboxDefaults.colors(checkedColor = HUDOrange))
                        Text("Mark as Owner (full control)", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        val profile = (editingProfile ?: FamilyProfileEntity(0, "", "", "", "", "", false, 0)).copy(
                            name = name.trim(), relation = relation.trim(),
                            accessLevel = if (isOwner) "full" else accessLevel,
                            notes = notes.trim(), isOwner = isOwner
                        )
                        memoryManager.saveFamilyProfile(profile)
                        showAddDialog = false; resetForm()
                    }
                }) { Text("Save", color = CyberBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; resetForm() }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }
}

@Composable
fun FamilyProfileCard(profile: FamilyProfileEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    val accessColor = when (profile.accessLevel) {
        "full" -> HUDOrange
        "limited" -> CyberBlue
        else -> TextSecondary
    }
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(GlassWhite, RoundedCornerShape(12.dp))
            .border(1.dp, if (profile.isOwner) HUDOrange.copy(0.4f) else GlassBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).background(if (profile.isOwner) HUDOrange.copy(0.15f) else GlassWhite, CircleShape)
            .border(1.dp, if (profile.isOwner) HUDOrange else CyberBlue.copy(0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(if (profile.isOwner) Icons.Default.Shield else Icons.Default.Person, null,
                tint = if (profile.isOwner) HUDOrange else CyberBlue, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(profile.name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                if (profile.isOwner) {
                    Spacer(Modifier.width(6.dp))
                    Text("OWNER", fontSize = 8.sp, color = HUDOrange, letterSpacing = 1.sp,
                        modifier = Modifier.background(HUDOrange.copy(0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp))
                }
            }
            Text(profile.relation, color = TextSecondary, fontSize = 11.sp)
            if (profile.notes.isNotEmpty()) Text(profile.notes, color = TextSecondary.copy(0.6f), fontSize = 10.sp, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(profile.accessLevel.uppercase(), color = accessColor, fontSize = 9.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, "Edit", tint = CyberBlue.copy(0.6f), modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, "Delete", tint = HUDOrange.copy(0.6f), modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
