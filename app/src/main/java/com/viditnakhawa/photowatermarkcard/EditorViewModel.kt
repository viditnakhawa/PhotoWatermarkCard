package com.viditnakhawa.photowatermarkcard

import android.app.Application
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

data class ExifData(
    val focalLength: String = "N/A",
    val aperture: String = "N/A",
    val shutterSpeed: String = "N/A",
    val iso: String = "N/A",
    val timestamp: String = "N/A"
)

/**
 * Represents the complete state for editor screen.
 */
data class EditorUiState(
    val selectedImageUri: Uri? = null,
    val exifData: ExifData = ExifData(),
    val customDeviceName: String = "My OnePlus Device",
    val watermarkBgColor: Color = Color.Black.copy(alpha = 0.5f)
)

/**
 * The ViewModel for our EditorScreen. It manages the UI state.
 * AndroidViewModel to get access to the application context.
 */
class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState = _uiState.asStateFlow()

    fun onDeviceNameChanged(newName: String) {
        _uiState.update { it.copy(customDeviceName = newName) }
    }

    fun onImageSelected(uri: Uri?) {
        if (uri == null) return

        _uiState.update { it.copy(selectedImageUri = uri) }
        extractExifData(uri)
        // --- NEW: Extract dominant color when image is selected ---
        extractDominantColor(uri)
    }

    private fun extractDominantColor(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>().applicationContext
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }

                Palette.from(bitmap).generate { palette ->
                    val dominantColor = palette?.darkMutedSwatch?.rgb?.let { Color(it) }
                        ?: palette?.darkVibrantSwatch?.rgb?.let { Color(it) }
                        ?: Color.Black

                    _uiState.update {
                        it.copy(watermarkBgColor = dominantColor.copy(alpha = 0.5f))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(watermarkBgColor = Color.Black.copy(alpha = 0.5f)) }
            }
        }
    }


    private fun extractExifData(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { inputStream ->
                    val exifInterface = ExifInterface(inputStream)
                    val newExifData = ExifData(
                        focalLength = exifInterface.getAttribute(ExifInterface.TAG_FOCAL_LENGTH) ?: "N/A",
                        aperture = exifInterface.getAttribute(ExifInterface.TAG_F_NUMBER)
                            ?: exifInterface.getAttribute(ExifInterface.TAG_APERTURE_VALUE)?.let { formatAperture(it) }
                            ?: "N/A",
                        shutterSpeed = exifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let { formatShutterSpeed(it.toFloatOrNull()) } ?: "N/A",
                        iso = exifInterface.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS) ?: "N/A",
                        timestamp = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                            ?: exifInterface.getAttribute(ExifInterface.TAG_DATETIME)
                            ?: "N/A"
                    )
                    _uiState.update { it.copy(exifData = newExifData) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(exifData = ExifData()) }
            }
        }
    }

    private fun formatAperture(value: String): String {
        return "f/${value.toFloatOrNull()?.let { "%.1f".format(it) } ?: "N/A"}"
    }

    private fun formatShutterSpeed(speed: Float?): String {
        if (speed == null) return "N/A"
        return if (speed < 1.0f) {
            "1/${(1.0f / speed).toInt()}s"
        } else {
            "${speed.toInt()}s"
        }
    }

    private fun formatTimestamp(dateTimeString: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
            val formatter = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
            val date = parser.parse(dateTimeString)
            if (date != null) formatter.format(date) else "N/A"
        } catch (e: Exception) {
            "N/A"
        }
    }

    fun onSaveClicked() {
        // TODO: In Phase 5, implement the image saving logic
        println("Save button clicked!")
    }
}