package com.viditnakhawa.photowatermarkcard

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.viditnakhawa.photowatermarkcard.services.AutoFrameService
import androidx.core.content.edit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("AutoFramePrefs", Context.MODE_PRIVATE)
    val isServiceEnabled = remember { mutableStateOf(sharedPrefs.getBoolean("service_enabled", false)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Automation Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Text("Enable Background Service")
                Switch(
                    checked = isServiceEnabled.value,
                    onCheckedChange = { isEnabled ->
                        isServiceEnabled.value = isEnabled
                        sharedPrefs.edit { putBoolean("service_enabled", isEnabled) }
                        toggleService(context, isEnabled)
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "When enabled, this app will automatically create a framed copy of every new photo you take.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(0.9f)
            )
        }
    }
}

private fun toggleService(context: Context, enable: Boolean) {
    val intent = Intent(context, AutoFrameService::class.java)
    if (enable) {
        context.startForegroundService(intent)
    } else {
        context.stopService(intent)
    }
}