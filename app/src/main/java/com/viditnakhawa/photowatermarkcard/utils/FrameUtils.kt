@file:Suppress("DEPRECATION")

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
import androidx.core.graphics.createBitmap

object FrameUtils {
    suspend fun processImage(context: Context, imageUri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val originalBitmap = loadBitmapFromUri(context, imageUri) ?: return@withContext false

                val exifData = extractExifData(context, imageUri)

                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"

                //Create the framed bitmap using Canvas
                val framedBitmap = createFramedBitmap(originalBitmap, exifData, deviceName)

                saveBitmapToGallery(context, framedBitmap, deviceName)

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        }
    }

    private fun extractExifData(context: Context, uri: Uri): ExifData {
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
        return ExifData() // Return default if stream fails
    }

    private fun formatShutterSpeed(speed: Float?): String {
        if (speed == null) return "N/A"
        return if (speed < 1.0f) {
            "1/${(1.0f / speed).toInt()}s"
        } else {
            "${speed.toInt()}s"
        }
    }

    private fun createFramedBitmap(original: Bitmap, exif: ExifData, deviceName: String): Bitmap {
        // --- Define Frame & Text Properties ---
        val borderTopLeftRight = (original.width * 0.05f).toInt() // 5% border
        val borderBottom = (original.height * 0.25f).toInt() // 25% bottom chin

        val newWidth = original.width + (borderTopLeftRight * 2)
        val newHeight = original.height + borderTopLeftRight + borderBottom

        val bitmapConfig = original.config ?: Bitmap.Config.ARGB_8888
        val newBitmap = createBitmap(newWidth, newHeight, bitmapConfig)
        val canvas = Canvas(newBitmap)

        // --- Draw Background ---
        canvas.drawColor(Color.WHITE)

        // --- Draw Original Photo ---
        val photoRect = Rect(borderTopLeftRight, borderTopLeftRight, newWidth - borderTopLeftRight, newHeight - borderBottom)
        canvas.drawBitmap(original, null, photoRect, null)

        val deviceNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = newWidth * 0.05f // Responsive text size
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val metadataPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = newWidth * 0.025f // Responsive text size
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        // --- Draw Text ---
        val textBaseX = canvas.width / 2f
        val deviceNameY = photoRect.bottom + (borderBottom * 0.45f)
        val metadataY = deviceNameY + (borderBottom * 0.25f)

        canvas.drawText(deviceName, textBaseX, deviceNameY, deviceNamePaint)

        val metadataText = "${exif.focalLength} | ${exif.aperture} | ${exif.shutterSpeed} | ISO${exif.iso}"
        canvas.drawText(metadataText, textBaseX, metadataY, metadataPaint)

        return newBitmap
    }

    private suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap, deviceName: String) {
        withContext(Dispatchers.IO) {
            val filename = "AutoFrame_${deviceName.replace(" ", "_")}_${System.currentTimeMillis()}.jpg"
            val fos: OutputStream?
            var imageUri: Uri? = null

            try {
                val picturesDirectory =
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val autoFramedDir = "Pictures/AutoFramed"

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, autoFramedDir)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val resolver = context.contentResolver
                imageUri = resolver.insert(picturesDirectory, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }

                fos?.use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    imageUri?.let { resolver.update(it, contentValues, null, null) }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Image saved to 'AutoFramed' album!", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to save image.", Toast.LENGTH_SHORT).show()
                }
            } finally {
                imageUri?.let {
                    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, it)
                    context.sendBroadcast(mediaScanIntent)
                }
            }
        }
    }
}
