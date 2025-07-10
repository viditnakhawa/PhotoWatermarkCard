package com.viditnakhawa.photowatermarkcard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.viditnakhawa.photowatermarkcard.navigation.AppNavigation
import com.viditnakhawa.photowatermarkcard.services.AutoFrameService
import com.viditnakhawa.photowatermarkcard.ui.theme.PhotoWatermarkCardTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isReadImagesGranted = permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false

        if (isReadImagesGranted) {
            val sharedPrefs = getSharedPreferences("AutoFramePrefs", Context.MODE_PRIVATE)
            if (sharedPrefs.getBoolean("service_enabled", false)) {
                toggleService(this, true)
            }
        } else {
            Toast.makeText(this, "Full photo access is recommended for automatic framing.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissionsIfNeeded()

        setContent {
            PhotoWatermarkCardTheme {
                AppNavigation()
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        } else { // Below Android 13
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val permissionsNotGranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNotGranted.isNotEmpty()) {
            permissionLauncher.launch(permissionsNotGranted.toTypedArray())
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
}
