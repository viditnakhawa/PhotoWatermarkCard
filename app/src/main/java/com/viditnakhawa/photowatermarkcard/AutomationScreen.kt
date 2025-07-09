package com.viditnakhawa.photowatermarkcard

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.viditnakhawa.photowatermarkcard.services.AutoFrameService
import androidx.core.content.edit

@Composable
fun AutomationScreen() {
    val context = LocalContext.current
    //SharedPreferences to remember if the service is on/off
    val sharedPrefs = context.getSharedPreferences("AutoFramePrefs", Context.MODE_PRIVATE)
    val isServiceEnabled = remember { mutableStateOf(sharedPrefs.getBoolean("service_enabled", false)) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Automatic Photo Framing", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(0.8f)
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
            modifier = Modifier.fillMaxWidth(0.8f)
        )
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
