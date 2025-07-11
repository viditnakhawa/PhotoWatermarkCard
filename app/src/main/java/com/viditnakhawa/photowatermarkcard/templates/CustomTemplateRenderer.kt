package com.viditnakhawa.photowatermarkcard.templates

import android.content.Context
import android.graphics.*
import android.os.Build
import androidx.annotation.RequiresApi
import blurBitmap
import com.viditnakhawa.photowatermarkcard.ExifData
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

/**
 * Renders the classic "Polaroid" style frame.
 * This is the default template, compatible with all Android versions.
 * It features a prominent white border with centered text at the bottom.
 */
class PolaroidRenderer : TemplateRenderer {
    override fun render(context: Context, original: Bitmap, exif: ExifData, deviceName: String): Bitmap {
        // --- Define Frame Dimensions ---
        val borderTopLeftRight = (original.width * 0.05f).toInt()
        val borderBottom = (original.height * 0.25f).toInt()
        val newWidth = original.width + (borderTopLeftRight * 2)
        val newHeight = original.height + borderTopLeftRight + borderBottom

        // --- Create Canvas and Draw Base ---
        val result = createBitmap(newWidth, newHeight, original.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        // --- Draw the Original Photo ---
        val photoRect = Rect(borderTopLeftRight, borderTopLeftRight, newWidth - borderTopLeftRight, newHeight - borderBottom)
        canvas.drawBitmap(original, null, photoRect, null)

        // --- Handle HDR Gainmap (if available) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            handleGainmap(original, result, photoRect)
        }

        // --- Prepare Paint for Text ---
        val deviceNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = newWidth * 0.045f
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val metadataPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = newWidth * 0.025f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        // --- Draw Text ---
        val textCenterX = canvas.width / 2f
        val deviceNameY = photoRect.bottom + (borderBottom * 0.45f)
        val metadataY = deviceNameY + (borderBottom * 0.3f)
        canvas.drawText(deviceName, textCenterX, deviceNameY, deviceNamePaint)
        val metadataText = "${exif.focalLength} | ${exif.aperture} | ${exif.shutterSpeed} | ISO ${exif.iso}"
        canvas.drawText(metadataText, textCenterX, metadataY, metadataPaint)

        return result
    }
}

/**
 * Renders the "Aero Blue" style, featuring the photo on a blurred background.
 * This template requires API 31+ to use the new BitmapBlur implementation.
 */
@RequiresApi(Build.VERSION_CODES.S)
class AeroBlueRenderer : TemplateRenderer {
    override fun render(context: Context, original: Bitmap, exif: ExifData, deviceName: String): Bitmap {
        // --- Create Blurred Background using the new BitmapBlur class ---
        val scaledBitmap =
            original.scale((original.width * 1.1f).toInt(), (original.height * 1.1f).toInt())
        val blurredBackground = blurBitmap(context, scaledBitmap, 25f)

        // --- Create Canvas and Draw Base ---
        val result = createBitmap(
            blurredBackground.width,
            blurredBackground.height,
            original.config ?: Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(result)
        canvas.drawBitmap(blurredBackground, 0f, 0f, null)

        // --- Draw Original Photo in the Center ---
        val photoRect = Rect(
            (result.width - original.width) / 2,
            (result.height - original.height) / 2,
            (result.width + original.width) / 2,
            (result.height + original.height) / 2
        )
        canvas.drawBitmap(original, null, photoRect, null)

        // --- Prepare Paint for Text ---
        val padding = result.width * 0.04f
        val paintDevice = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = result.width * 0.04f
            typeface = Typeface.create("serif", Typeface.BOLD)
            setShadowLayer(5f, 2f, 2f, Color.argb(128, 0, 0, 0))
        }
        val paintExif = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 200
            textSize = result.width * 0.025f
            typeface = Typeface.create("monospace", Typeface.NORMAL)
            setShadowLayer(5f, 2f, 2f, Color.argb(128, 0, 0, 0))
        }

        // --- Draw Text in Bottom Left ---
        val textBounds = Rect()
        paintExif.getTextBounds("A", 0, 1, textBounds)
        val exifLineHeight = textBounds.height() * 1.5f
        val deviceNameY = result.height - padding - (exifLineHeight * 2)
        canvas.drawText(deviceName, padding, deviceNameY, paintDevice)
        canvas.drawText(
            "${exif.focalLength} f/${exif.aperture} ${exif.shutterSpeed} ISO${exif.iso}",
            padding,
            deviceNameY + exifLineHeight,
            paintExif
        )
        canvas.drawText(formatTimestamp(exif.timestamp), padding, deviceNameY + (exifLineHeight * 2), paintExif)

        // --- Handle HDR Gainmap ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            handleGainmap(original, result, photoRect)
        }
        return result
    }
}

/**
 * Renders the "Bottom Bar" style.
 */
class BottomBarRenderer : TemplateRenderer {
    override fun render(context: Context, original: Bitmap, exif: ExifData, deviceName: String): Bitmap {
        // --- Define Dimensions ---
        val barHeight = (original.height * 0.15).toInt()
        val newHeight = original.height + barHeight

        // --- Create a new, taller bitmap ---
        val result =
            createBitmap(original.width, newHeight, original.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        // --- Draw the original image at the top ---
        canvas.drawBitmap(original, 0f, 0f, null)

        // --- Prepare Paint for Text ---
        val padding = original.width * 0.04f
        val paintDevice = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = original.width * 0.035f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        val paintExif = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = original.width * 0.022f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }

        // --- Draw Text in the new bottom bar area ---
        val textBounds = Rect()
        paintDevice.getTextBounds("A", 0, 1, textBounds)
        val deviceLineHeight = textBounds.height() * 1.5f
        val baseY = original.height + (barHeight - deviceLineHeight * 2) / 2 + deviceLineHeight
        canvas.drawText(deviceName, padding, baseY, paintDevice)
        canvas.drawText(
            "${exif.focalLength}  f/${exif.aperture}  ${exif.shutterSpeed}  ISO${exif.iso}",
            padding,
            baseY + deviceLineHeight,
            paintExif
        )

        // --- Handle HDR Gainmap ---
        val photoRect = Rect(0, 0, original.width, original.height)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            handleGainmap(original, result, photoRect)
        }
        return result
    }
}

/**
 * Renders the "Sunset" style, featuring minimal borders and left-aligned text.
 */
class SunsetRenderer : TemplateRenderer {
    override fun render(context: Context, original: Bitmap, exif: ExifData, deviceName: String): Bitmap {
        // --- Define Frame Dimensions ---
        val border = (original.width * 0.02).toInt()
        val captionHeight = (original.height * 0.15).toInt()
        val newWidth = original.width + 2 * border
        val newHeight = original.height + border + captionHeight
        val result = createBitmap(newWidth, newHeight)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        // --- Draw the Original Photo ---
        val photoRect = Rect(border, border, newWidth - border, newHeight - captionHeight)
        canvas.drawBitmap(original, null, photoRect, null)

        // --- Prepare Paint for Text ---
        val padding = border.toFloat()
        val paintDevice = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = newWidth * 0.035f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val paintExif = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = newWidth * 0.022f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }

        // --- Draw Text ---
        val textBounds = Rect()
        paintDevice.getTextBounds("A", 0, 1, textBounds)
        val deviceLineHeight = textBounds.height() * 1.5f
        val baseY = photoRect.bottom + (captionHeight - deviceLineHeight * 2) / 2 + deviceLineHeight
        canvas.drawText(deviceName, padding, baseY, paintDevice)
        canvas.drawText(
            "● ${exif.focalLength} / f${exif.aperture} / ${exif.shutterSpeed} / ISO${exif.iso}",
            padding,
            baseY + deviceLineHeight,
            paintExif
        )

        // --- Handle HDR Gainmap ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            handleGainmap(original, result, photoRect)
        }
        return result
    }
}


// --- SHARED HELPER FUNCTIONS ---

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private fun handleGainmap(original: Bitmap, result: Bitmap, photoRect: Rect) {
    original.gainmap?.let { originalGainmap ->
        val originalGainmapContents = originalGainmap.gainmapContents
        val gainmapConfig = originalGainmapContents.config ?: Bitmap.Config.ALPHA_8
        if (gainmapConfig != Bitmap.Config.HARDWARE) {
            val newGainmapContents = createBitmap(result.width, result.height, gainmapConfig)
            val canvas = Canvas(newGainmapContents)
            canvas.drawBitmap(originalGainmapContents, null, photoRect, null)
            result.gainmap = Gainmap(newGainmapContents)
        }
    }
}

private fun formatTimestamp(dateTimeString: String): String {
    if (dateTimeString.isBlank() || dateTimeString == "N/A") return ""
    return try {
        val parser = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
        val formatter = SimpleDateFormat("d MMMM yyyy 'at' HH:mm", Locale.getDefault())
        val date = parser.parse(dateTimeString)
        if (date != null) formatter.format(date) else ""
    } catch (e: Exception) {
        ""
    }
}
