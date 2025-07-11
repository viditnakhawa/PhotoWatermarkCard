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
 * A utility object for processing images. It orchestrates the process of loading an image,
 * applying a selected frame template via its specific renderer, and saving the result.
 */
object FrameUtils {

    /**
     * The main function to process an image from a given URI.
     * It loads the image, finds the appropriate template renderer, delegates the drawing,
     * and saves the final result to the gallery.
     *
     * @param context The application context.
     * @param imageUri The URI of the image to process.
     * @return True if the image was processed and saved successfully, false otherwise.
     */
    suspend fun processImage(context: Context, imageUri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Load user preferences to find the selected template ID. Default to "polaroid".
                val sharedPrefs = context.getSharedPreferences("AutoFramePrefs", Context.MODE_PRIVATE)
                val selectedTemplateId = sharedPrefs.getString("selected_template_id", "polaroid")

                // 2. Find the template from the repository.
                val template = TemplateRepository.findById(selectedTemplateId)
                if (template == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "No templates available on this device.", Toast.LENGTH_LONG).show()
                    }
                    return@withContext false
                }

                // 3. Load the source bitmap and its EXIF data.
                val originalBitmap = loadBitmapFromUri(context, imageUri) ?: return@withContext false
                val exifDataForDisplay = extractExifDataForDisplay(context, imageUri)
                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"

                // 4. Delegate the entire rendering process to the template's specific renderer.
                val framedBitmap = template.renderer.render(context, originalBitmap, exifDataForDisplay, deviceName)

                // 5. Save the final, framed bitmap to the gallery.
                saveBitmapToGallery(context, framedBitmap, deviceName)

                true
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "An error occurred during processing.", Toast.LENGTH_SHORT).show()
                }
                false
            }
        }
    }

    /**
     * Loads a Bitmap from a Uri, ensuring it's mutable.
     */
    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
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
     * Extracts a subset of EXIF data from the image URI for display.
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
        return ExifData() // Return default empty data on failure
    }

    /**
     * Formats a shutter speed float (e.g., 0.004) into a fractional string (e.g., "1/250s").
     */
    private fun formatShutterSpeed(speed: Float?): String {
        if (speed == null) return "N/A"
        return if (speed < 1.0f) "1/${(1.0f / speed).toInt()}s" else "${speed.toInt()}s"
    }

    /**
     * Saves the final bitmap to the device's public gallery in the "Pictures/AutoFramed" directory.
     */
    private suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap, deviceName: String) {
        withContext(Dispatchers.IO) {
            val filename = "AutoFrame_${deviceName.replace(" ", "_")}_${System.currentTimeMillis()}.jpg"
            val mimeType = "image/jpeg"
            var imageUri: Uri? = null
            val resolver = context.contentResolver

            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/AutoFramed")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                imageUri?.let { uri ->
                    resolver.openOutputStream(uri)?.use { fos ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Image saved to 'AutoFramed' album!", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { Toast.makeText(context, "Failed to save image.", Toast.LENGTH_SHORT).show() }
            } finally {
                imageUri?.let { context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, it)) }
            }
        }
    }
}

/*
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Gainmap
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import androidx.exifinterface.media.ExifInterface
import com.viditnakhawa.photowatermarkcard.ExifData
import com.viditnakhawa.photowatermarkcard.templates.FrameTemplate
import com.viditnakhawa.photowatermarkcard.templates.TemplateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A utility object for processing images to add a custom frame and metadata.
 * It includes specialized handling for Ultra HDR images (JPEG_R with Gainmaps)
 * on Android 14+ to preserve HDR data correctly after composition.
 */
object FrameUtils {

    /**
     * The main function to process an image from a given URI.
     * It loads the image, creates a new bitmap with a frame and text,
     * correctly handles the gainmap for HDR images, and saves the result to the gallery.
     *
     * @param context The application context.
     * @param imageUri The URI of the image to process.
     * @return True if the image was processed and saved successfully, false otherwise.
     */
    suspend fun processImage(context: Context, imageUri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Load user preferences and device info
                val sharedPrefs = context.getSharedPreferences("AutoFramePrefs", Context.MODE_PRIVATE)
                val selectedTemplateId = sharedPrefs.getString("selected_template_id", "classic_white") ?: "classic_white"
                val template = TemplateRepository.findById(selectedTemplateId)
                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"

                // Load the source bitmap using a modern decoder to preserve properties like gainmaps
                val originalBitmap = loadBitmapFromUri(context, imageUri) ?: return@withContext false

                // Extract EXIF data for display on the frame
                val exifDataForDisplay = extractExifDataForDisplay(context, imageUri)

                // Create the new framed bitmap. This function now handles the complex gainmap transformation.
                val framedBitmap = createFramedBitmap(context, originalBitmap, exifDataForDisplay, deviceName, template)

                // Save the final image to the gallery.
                saveBitmapToGallery(context, framedBitmap, deviceName, imageUri)

                true
            } catch (e: Exception) {
                e.printStackTrace()
                // Inform the user on the main thread if an error occurs
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "An error occurred during processing.", Toast.LENGTH_SHORT).show()
                }
                false
            }
        }
    }

    /**
     * Loads a Bitmap from a Uri, ensuring it's mutable and preserves HDR gainmaps.
     */
    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    // Ensure the bitmap is mutable
                    decoder.isMutableRequired = true
                }
            } else {
                // Fallback for older Android versions
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Extracts a subset of EXIF data from the image URI for display purposes.
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
        return ExifData() // Return default empty data on failure
    }

    /**
     * Formats a shutter speed float (e.g., 0.004) into a fractional string (e.g., "1/250s").
     */
    private fun formatShutterSpeed(speed: Float?): String {
        if (speed == null) return "N/A"
        return if (speed < 1.0f) "1/${(1.0f / speed).toInt()}s" else "${speed.toInt()}s"
    }

    private fun createFramedBitmap(
        context: Context,
        original: Bitmap,
        exif: ExifData,
        deviceName: String,
        template: FrameTemplate
    ): Bitmap {
        // Calculate border sizes based on image dimensions
        val borderTopLeftRight = (original.width * 0.05f).toInt()
        val borderBottom = (original.height * 0.25f).toInt()
        val newWidth = original.width + (borderTopLeftRight * 2)
        val newHeight = original.height + borderTopLeftRight + borderBottom

        // Create the new, larger bitmap for final image
        val newBitmap = Bitmap.createBitmap(newWidth, newHeight, original.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBitmap)
        canvas.drawColor(template.frameColor.toArgb())

        // Define the rectangle where the original photo will be drawn
        val photoRect = Rect(borderTopLeftRight, borderTopLeftRight, newWidth - borderTopLeftRight, newHeight - borderBottom)
        canvas.drawBitmap(original, null, photoRect, null)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            original.gainmap?.let { originalGainmap ->
                // 1. Get the bitmap that represents the original gainmap's content.
                val originalGainmapContents = originalGainmap.gainmapContents

                val gainmapConfig = originalGainmapContents.config ?: Bitmap.Config.ARGB_8888
                if (gainmapConfig != Bitmap.Config.HARDWARE) {

                    // 2. Create a new, larger, blank bitmap for new gainmap, matching the final image size.
                    val newGainmapContents = Bitmap.createBitmap(
                        newWidth,
                        newHeight,
                        gainmapConfig
                    )

                    // 3. Draw the original gainmap's content into the new gainmap bitmap at the correct offset (photoRect).
                    val gainmapCanvas = Canvas(newGainmapContents)
                    gainmapCanvas.drawBitmap(originalGainmapContents, null, photoRect, null)

                    // 4. Create a new Gainmap object from our transformed bitmap.
                    val newGainmap = Gainmap(newGainmapContents)

                    // 5. Attach the new, correctly positioned gainmap to final framed bitmap.
                    newBitmap.gainmap = newGainmap
                }
            }
        }

        val deviceNameTypeface = if (template.deviceNameFontResId != null) {
            ResourcesCompat.getFont(context, template.deviceNameFontResId)
        } else {
            Typeface.create("sans-serif-condensed", Typeface.BOLD)
        }

        val deviceNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = template.deviceNameTextColor.toArgb()
            textSize = newWidth * 0.05f
            typeface = deviceNameTypeface
            textAlign = Paint.Align.CENTER
        }
        val metadataPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = template.metadataTextColor.toArgb()
            textSize = newWidth * 0.025f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        // Calculate text positions and draw them on the canvas
        val textBaseX = canvas.width / 2f
        val deviceNameY = photoRect.bottom + (borderBottom * 0.45f)
        val metadataY = deviceNameY + (borderBottom * 0.25f)
        canvas.drawText(deviceName, textBaseX, deviceNameY, deviceNamePaint)
        val metadataText = "${exif.focalLength} | ${exif.aperture} | ${exif.shutterSpeed} | ISO${exif.iso}"
        canvas.drawText(metadataText, textBaseX, metadataY, metadataPaint)

        return newBitmap
    }

    /**
     * Saves the final bitmap to the device's public gallery in the "Pictures/AutoFramed" directory.
     * It automatically saves as JPEG_R if a gainmap is present on the bitmap.
     */
    private suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap, deviceName: String, originalUri: Uri) {
        withContext(Dispatchers.IO) {
            val filename = "AutoFrame_${deviceName.replace(" ", "_")}_${System.currentTimeMillis()}.jpg"
            val mimeType = "image/jpeg"
            var imageUri: Uri? = null
            val resolver = context.contentResolver

            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/AutoFramed")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                imageUri?.let { uri ->
                    resolver.openOutputStream(uri)?.use { fos ->
                        // The system automatically saves as JPEG_R if bitmap.hasGainmap() is true.
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
                    }

                    // Mark the image as no longer pending so it's visible to other apps.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Image saved to 'AutoFramed' album!", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { Toast.makeText(context, "Failed to save image.", Toast.LENGTH_SHORT).show() }
            } finally {
                // Trigger a media scan to ensure the file is immediately visible in the gallery.
                imageUri?.let { context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, it)) }
            }
        }
    }
}*/
