package com.viditnakhawa.photowatermarkcard.templates

import android.content.Context
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrameTemplatesScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("AutoFramePrefs", Context.MODE_PRIVATE) }
    var selectedTemplateId by remember { mutableStateOf(sharedPrefs.getString("selected_template_id", "polaroid")) }
    var showDeviceNameDialog by remember { mutableStateOf(false) }

    var isSunsetGradientEnabled by remember {
        mutableStateOf(sharedPrefs.getBoolean("sunset_gradient_enabled", true))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose a Template") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { IconButton(onClick = { showDeviceNameDialog = true }) { Icon(Icons.Outlined.PhoneAndroid, "Edit Device Name") } }
            )
        }
    ) { paddingValues ->
        if (showDeviceNameDialog) {
            DeviceNameEditDialog(
                onDismiss = { showDeviceNameDialog = false },
                onSave = { newName ->
                    sharedPrefs.edit { putString("custom_device_model", newName) }
                    showDeviceNameDialog = false
                }
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(TemplateRepository.templates) { template ->
                TemplateListItem(
                    template = template,
                    isSelected = template.id == selectedTemplateId,
                    onTemplateSelected = {
                        selectedTemplateId = it
                        sharedPrefs.edit { putString("selected_template_id", it) }
                    },
                    isSunsetGradientEnabled = isSunsetGradientEnabled,
                    onSunsetGradientToggle = { enable ->
                        isSunsetGradientEnabled = enable
                        // Save the user's choice
                        sharedPrefs.edit { putBoolean("sunset_gradient_enabled", enable) }
                    }
                )
            }
        }
    }
}

@Composable
private fun DeviceNameEditDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("AutoFramePrefs", Context.MODE_PRIVATE) }
    val currentModel = remember { sharedPrefs.getString("custom_device_model", null) ?: Build.MODEL }
    var text by remember { mutableStateOf(currentModel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Device Model") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Custom model name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TemplateListItem(
    template: FrameTemplate,
    isSelected: Boolean,
    onTemplateSelected: (String) -> Unit,
    isSunsetGradientEnabled: Boolean,
    onSunsetGradientToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.clickable { onTemplateSelected(template.id) }) {

            val imageRes = if (template.id == "sunset" && !isSunsetGradientEnabled) {
                template.disabledPreviewImageRes ?: template.previewImageRes // Fallback just in case
            } else {
                template.previewImageRes
            }

            AsyncImage(
                model = imageRes,
                contentDescription = "${template.name} preview",
                contentScale = ContentScale.Fit, // ensures full image is visible
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp) // standard fixed height for all previews
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                if (template.id == "sunset") {
                    Switch(
                        checked = isSunsetGradientEnabled,
                        onCheckedChange = onSunsetGradientToggle,
                        thumbContent = null,
                        modifier = Modifier.height(20.dp)
                    )
                }
            }
        }
    }
}