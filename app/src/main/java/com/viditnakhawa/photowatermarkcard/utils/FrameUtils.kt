package com.viditnakhawa.photowatermarkcard.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
import com.viditnakhawa.photowatermarkcard.ExifData
import com.viditnakhawa.photowatermarkcard.templates.TemplateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ----------------------------------------------------
 * FrameUtils
 * ----------------------------------------------------
 * A utility object that:
 * - Loads a photo from a URI
 * - Extracts EXIF data (focal length, aperture, etc.)
 * - Applies a selected frame template (Polaroid, Aero, etc.)
 * - Saves the final image to the gallery
 *
 * This handles the entire framing pipeline end-to-end.
 */
object FrameUtils {

    /**
     * Main entry point to process an image by framing it.
     *
     * @param context The Android Context
     * @param imageUri URI of the source image to be framed
     * @param templateId Optional specific template ID (e.g., "sunset"). If null, the default saved in preferences is used.
     * @return True if processing and saving succeeded, false otherwise
     */
    suspend fun processImage(context: Context, imageUri: Uri, templateId: String? = null): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // --- Select template ID ---
                val finalTemplateId = if (templateId != null) {
                    templateId
                } else {
                    val sharedPrefs = context.getSharedPreferences("AutoFramePrefs", Context.MODE_PRIVATE)
                    sharedPrefs.getString("selected_template_id", "polaroid") // fallback to "polaroid"
                }

                // --- Find the corresponding template from the repository ---
                val template = TemplateRepository.findById(finalTemplateId)
                if (template == null) {
                    // Show error on UI thread
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Template not found.", Toast.LENGTH_LONG).show()
                    }
                    return@withContext false
                }

                // --- Load the image from URI into a Bitmap ---
                val originalBitmap = loadBitmapFromUri(context, imageUri) ?: return@withContext false

                // --- Extract EXIF metadata for rendering ---
                val exifDataForDisplay = extractExifDataForDisplay(context, imageUri)

                // --- Build final device name (allow custom override) ---
                val sharedPrefs = context.getSharedPreferences("AutoFramePrefs", Context.MODE_PRIVATE)
                val customModel = sharedPrefs.getString("custom_device_model", null)
                val modelName = if (!customModel.isNullOrBlank()) customModel else Build.MODEL
                val deviceName = "${Build.MANUFACTURER} $modelName"

                // --- Delegate to the selected template renderer ---
                val framedBitmap = template.renderer.render(context, originalBitmap, exifDataForDisplay, deviceName)

                // --- Save the result to MediaStore/Gallery ---
                saveBitmapToGallery(context, framedBitmap, deviceName, imageUri)

                return@withContext true

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "An error occurred during processing.", Toast.LENGTH_SHORT).show()
                }
                return@withContext false
            }
        }
    }

    /**
     * Loads a mutable Bitmap from a given URI.
     *
     * Uses ImageDecoder (API 28+) or MediaStore (pre-API 28).
     */
    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true // ensures we can draw on it later
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Extracts relevant EXIF tags (focal length, aperture, shutter speed, ISO, timestamp).
     *
     * Used for drawing captions in the framed image.
     */
    private fun extractExifDataForDisplay(context: Context, uri: Uri): ExifData {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exifInterface = ExifInterface(inputStream)
                return ExifData(
                    focalLength = exifInterface.getAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM) ?: "N/A",
                    aperture = exifInterface.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" } ?: "N/A",
                    shutterSpeed = exifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let { formatShutterSpeed(it.toFloatOrNull()) } ?: "N/A",
                    iso = exifInterface.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS) ?: "N/A",
                    timestamp = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?: ""
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Return a default placeholder on failure
        return ExifData()
    }

    /**
     * Converts a shutter speed (float seconds) to a human-readable string.
     * Example: 0.004f → "1/250s"
     */
    private fun formatShutterSpeed(speed: Float?): String {
        if (speed == null) return "N/A"
        return if (speed < 1.0f) {
            "1/${(1.0f / speed).toInt()}s"
        } else {
            "${speed.toInt()}s"
        }
    }

    /**
     * Saves the final framed Bitmap to the system gallery using MediaStore.
     *
     * - Creates a new file in "Pictures/AutoFramed"
     * - Uses MediaStore API (Q+ compatible)
     * - Triggers media scanner to make the file visible
     */
    private suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap, deviceName: String, originalUri: Uri) {
        withContext(Dispatchers.IO) {
            val filename = "AutoFrame_${deviceName.replace(" ", "_")}_${System.currentTimeMillis()}.jpg"
            val mimeType = "image/jpeg"
            var imageUri: Uri? = null
            val resolver = context.contentResolver

            try {
                // --- Prepare MediaStore values ---
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/AutoFramed")
                        put(MediaStore.MediaColumns.IS_PENDING, 1) // mark as pending while writing
                    }
                }

                // --- Insert into MediaStore ---
                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                imageUri?.let { uri ->
                    resolver.openOutputStream(uri)?.use { fos ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
                    }

                    // --- Mark file as ready (remove IS_PENDING flag) ---
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    }
                    ExifUtils.copyExifData(context, originalUri, uri)
                }

                // --- Notify user and refresh gallery ---
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Image saved to 'AutoFramed' album!", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to save image.", Toast.LENGTH_SHORT).show()
                }

            } finally {
                // --- Force media scanner to detect file immediately (pre-API 29 fallback) ---
                imageUri?.let {
                    context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, it))
                }
            }
        }
    }
}
