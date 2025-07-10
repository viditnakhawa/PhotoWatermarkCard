package com.viditnakhawa.photowatermarkcard.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
import com.viditnakhawa.photowatermarkcard.ExifData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

object FrameUtils {

    /**
     * This is the main function that orchestrates the entire framing process.
     */
    suspend fun processImage(context: Context, imageUri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Step 1: Load original Bitmap from URI
                val originalBitmap = loadBitmapFromUri(context, imageUri) ?: return@withContext false

                // Step 2: Extract EXIF data from URI for display
                val exifDataForDisplay = extractExifDataForDisplay(context, imageUri)

                // Step 3: Get device name
                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"

                // Step 4: Create the framed bitmap using Canvas
                val framedBitmap = createFramedBitmap(originalBitmap, exifDataForDisplay, deviceName)

                // Step 5: Save the new bitmap to the gallery, passing the original URI to copy metadata from
                saveBitmapToGallery(context, framedBitmap, deviceName, imageUri)

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    }

    /**
     * Extracts only the EXIF data needed for the visual text on the frame.
     */
    private fun extractExifDataForDisplay(context: Context, uri: Uri): ExifData {
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
        return ExifData()
    }

    private fun formatShutterSpeed(speed: Float?): String {
        if (speed == null) return "N/A"
        return if (speed < 1.0f) "1/${(1.0f / speed).toInt()}s" else "${speed.toInt()}s"
    }

    private fun createFramedBitmap(original: Bitmap, exif: ExifData, deviceName: String): Bitmap {
        val borderTopLeftRight = (original.width * 0.05f).toInt()
        val borderBottom = (original.height * 0.25f).toInt()
        val newWidth = original.width + (borderTopLeftRight * 2)
        val newHeight = original.height + borderTopLeftRight + borderBottom
        val bitmapConfig = original.config ?: Bitmap.Config.ARGB_8888
        val newBitmap = Bitmap.createBitmap(newWidth, newHeight, bitmapConfig)
        val canvas = Canvas(newBitmap)
        canvas.drawColor(Color.WHITE)
        val photoRect = Rect(borderTopLeftRight, borderTopLeftRight, newWidth - borderTopLeftRight, newHeight - borderBottom)
        canvas.drawBitmap(original, null, photoRect, null)
        val deviceNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = newWidth * 0.05f
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val metadataPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = newWidth * 0.025f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val textBaseX = canvas.width / 2f
        val deviceNameY = photoRect.bottom + (borderBottom * 0.45f)
        val metadataY = deviceNameY + (borderBottom * 0.25f)
        canvas.drawText(deviceName, textBaseX, deviceNameY, deviceNamePaint)
        val metadataText = "${exif.focalLength} | ${exif.aperture} | ${exif.shutterSpeed} | ISO${exif.iso}"
        canvas.drawText(metadataText, textBaseX, metadataY, metadataPaint)
        return newBitmap
    }

    private suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap, deviceName: String, originalUri: Uri) {
        withContext(Dispatchers.IO) {
            val filename = "AutoFrame_${deviceName.replace(" ", "_")}_${System.currentTimeMillis()}.jpg"
            var imageUri: Uri? = null
            val resolver = context.contentResolver

            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/AutoFramed")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                imageUri?.let { uri ->
                    resolver.openOutputStream(uri)?.use { fos ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
                    }

                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)

                    // --- Copy EXIF data from original to new file ---
                    copyExifData(context, originalUri, uri)
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

    /**
     * Copies all available EXIF data from a source URI to a destination URI.
     */
    private fun copyExifData(context: Context, sourceUri: Uri, destUri: Uri) {
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { sourceStream ->
                val sourceExif = ExifInterface(sourceStream)

                context.contentResolver.openFileDescriptor(destUri, "rw")?.use { destFd ->
                    val destExif = ExifInterface(destFd.fileDescriptor)

                    // A comprehensive list of EXIF tags to preserve
                    val tags = arrayOf(
                        ExifInterface.TAG_APERTURE_VALUE,
                        ExifInterface.TAG_ARTIST,
                        ExifInterface.TAG_BITS_PER_SAMPLE,
                        ExifInterface.TAG_COMPRESSION,
                        ExifInterface.TAG_CONTRAST,
                        ExifInterface.TAG_COPYRIGHT,
                        ExifInterface.TAG_DATETIME,
                        ExifInterface.TAG_DATETIME_DIGITIZED,
                        ExifInterface.TAG_DATETIME_ORIGINAL,
                        ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION,
                        ExifInterface.TAG_DIGITAL_ZOOM_RATIO,
                        ExifInterface.TAG_EXIF_VERSION,
                        ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
                        ExifInterface.TAG_EXPOSURE_INDEX,
                        ExifInterface.TAG_EXPOSURE_MODE,
                        ExifInterface.TAG_EXPOSURE_PROGRAM,
                        ExifInterface.TAG_EXPOSURE_TIME,
                        ExifInterface.TAG_FILE_SOURCE,
                        ExifInterface.TAG_FLASH,
                        ExifInterface.TAG_FLASH_ENERGY,
                        ExifInterface.TAG_FOCAL_LENGTH,
                        ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
                        ExifInterface.TAG_FOCAL_PLANE_RESOLUTION_UNIT,
                        ExifInterface.TAG_FOCAL_PLANE_X_RESOLUTION,
                        ExifInterface.TAG_FOCAL_PLANE_Y_RESOLUTION,
                        ExifInterface.TAG_F_NUMBER,
                        ExifInterface.TAG_GAIN_CONTROL,
                        ExifInterface.TAG_GPS_ALTITUDE,
                        ExifInterface.TAG_GPS_ALTITUDE_REF,
                        ExifInterface.TAG_GPS_AREA_INFORMATION,
                        ExifInterface.TAG_GPS_DATESTAMP,
                        ExifInterface.TAG_GPS_DEST_BEARING,
                        ExifInterface.TAG_GPS_DEST_BEARING_REF,
                        ExifInterface.TAG_GPS_DEST_DISTANCE,
                        ExifInterface.TAG_GPS_DEST_DISTANCE_REF,
                        ExifInterface.TAG_GPS_DEST_LATITUDE,
                        ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
                        ExifInterface.TAG_GPS_DEST_LONGITUDE,
                        ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
                        ExifInterface.TAG_GPS_DIFFERENTIAL,
                        ExifInterface.TAG_GPS_DOP,
                        ExifInterface.TAG_GPS_IMG_DIRECTION,
                        ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
                        ExifInterface.TAG_GPS_LATITUDE,
                        ExifInterface.TAG_GPS_LATITUDE_REF,
                        ExifInterface.TAG_GPS_LONGITUDE,
                        ExifInterface.TAG_GPS_LONGITUDE_REF,
                        ExifInterface.TAG_GPS_MAP_DATUM,
                        ExifInterface.TAG_GPS_MEASURE_MODE,
                        ExifInterface.TAG_GPS_PROCESSING_METHOD,
                        ExifInterface.TAG_GPS_SATELLITES,
                        ExifInterface.TAG_GPS_SPEED,
                        ExifInterface.TAG_GPS_SPEED_REF,
                        ExifInterface.TAG_GPS_STATUS,
                        ExifInterface.TAG_GPS_TIMESTAMP,
                        ExifInterface.TAG_GPS_TRACK,
                        ExifInterface.TAG_GPS_TRACK_REF,
                        ExifInterface.TAG_GPS_VERSION_ID,
                        ExifInterface.TAG_IMAGE_DESCRIPTION,
                        ExifInterface.TAG_IMAGE_LENGTH,
                        ExifInterface.TAG_IMAGE_UNIQUE_ID,
                        ExifInterface.TAG_IMAGE_WIDTH,
                        ExifInterface.TAG_INTEROPERABILITY_INDEX,
                        ExifInterface.TAG_ISO_SPEED_RATINGS,
                        ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT,
                        ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH,
                        ExifInterface.TAG_LIGHT_SOURCE,
                        ExifInterface.TAG_MAKE,
                        ExifInterface.TAG_MAKER_NOTE,
                        ExifInterface.TAG_MAX_APERTURE_VALUE,
                        ExifInterface.TAG_METERING_MODE,
                        ExifInterface.TAG_MODEL,
                        ExifInterface.TAG_OECF,
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.TAG_PHOTOMETRIC_INTERPRETATION,
                        ExifInterface.TAG_PIXEL_X_DIMENSION,
                        ExifInterface.TAG_PIXEL_Y_DIMENSION,
                        ExifInterface.TAG_PLANAR_CONFIGURATION,
                        ExifInterface.TAG_PRIMARY_CHROMATICITIES,
                        ExifInterface.TAG_REFERENCE_BLACK_WHITE,
                        ExifInterface.TAG_RESOLUTION_UNIT,
                        ExifInterface.TAG_ROWS_PER_STRIP,
                        ExifInterface.TAG_SAMPLES_PER_PIXEL,
                        ExifInterface.TAG_SATURATION,
                        ExifInterface.TAG_SCENE_CAPTURE_TYPE,
                        ExifInterface.TAG_SCENE_TYPE,
                        ExifInterface.TAG_SENSING_METHOD,
                        ExifInterface.TAG_SHARPNESS,
                        ExifInterface.TAG_SHUTTER_SPEED_VALUE,
                        ExifInterface.TAG_SOFTWARE,
                        ExifInterface.TAG_SPATIAL_FREQUENCY_RESPONSE,
                        ExifInterface.TAG_SPECTRAL_SENSITIVITY,
                        ExifInterface.TAG_STRIP_BYTE_COUNTS,
                        ExifInterface.TAG_STRIP_OFFSETS,
                        ExifInterface.TAG_SUBJECT_AREA,
                        ExifInterface.TAG_SUBJECT_DISTANCE,
                        ExifInterface.TAG_SUBJECT_DISTANCE_RANGE,
                        ExifInterface.TAG_SUBJECT_LOCATION,
                        ExifInterface.TAG_SUBSEC_TIME,
                        ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
                        ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
                        ExifInterface.TAG_THUMBNAIL_IMAGE_LENGTH,
                        ExifInterface.TAG_THUMBNAIL_IMAGE_WIDTH,
                        ExifInterface.TAG_TRANSFER_FUNCTION,
                        ExifInterface.TAG_USER_COMMENT,
                        ExifInterface.TAG_WHITE_BALANCE,
                        ExifInterface.TAG_WHITE_POINT,
                        ExifInterface.TAG_X_RESOLUTION,
                        ExifInterface.TAG_Y_CB_CR_COEFFICIENTS,
                        ExifInterface.TAG_Y_CB_CR_POSITIONING,
                        ExifInterface.TAG_Y_CB_CR_SUB_SAMPLING,
                        ExifInterface.TAG_Y_RESOLUTION
                    )

                    for (tag in tags) {
                        val value = sourceExif.getAttribute(tag)
                        if (value != null) {
                            destExif.setAttribute(tag, value)
                        }
                    }
                    destExif.saveAttributes()
                    println("Successfully copied EXIF data.")
                }
            }
        } catch (e: Exception) {
            // It's okay if this fails, the image is already saved.
            e.printStackTrace()
            println("Could not copy EXIF data: ${e.message}")
        }
    }
}
