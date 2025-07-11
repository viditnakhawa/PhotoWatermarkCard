package com.viditnakhawa.photowatermarkcard.templates

import android.content.Context
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.viditnakhawa.photowatermarkcard.templates.FrameTemplate
import com.viditnakhawa.photowatermarkcard.templates.TemplateRepository
import androidx.core.content.edit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrameTemplatesScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("AutoFramePrefs", Context.MODE_PRIVATE) }
    var selectedTemplateId by remember { mutableStateOf(sharedPrefs.getString("selected_template_id", "classic_white")) }
    var showDeviceNameDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose a Template") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeviceNameDialog = true }) {
                        Icon(Icons.Outlined.PhoneAndroid, contentDescription = "Edit Device Name")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (showDeviceNameDialog) {
            DeviceNameEditDialog(
                onDismiss = { showDeviceNameDialog = false },
                onSave = { newName ->
                    sharedPrefs.edit().putString("custom_device_model", newName).apply()
                    showDeviceNameDialog = false
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(top = 8.dp)
        ) {
            items(TemplateRepository.templates) { template ->
                TemplateListItem(
                    template = template,
                    isSelected = template.id == selectedTemplateId,
                    onTemplateSelected = {
                        selectedTemplateId = it
                        // Save the selected template ID to SharedPreferences
                        sharedPrefs.edit { putString("selected_template_id", it) }
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
    onTemplateSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onTemplateSelected(template.id) },
        shape = MaterialTheme.shapes.medium,
        tonalElevation = if (isSelected) 4.dp else 1.dp,
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = template.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewFrameTemplatesScreen() {
    MaterialTheme {
        FrameTemplatesScreen {}
    }
}