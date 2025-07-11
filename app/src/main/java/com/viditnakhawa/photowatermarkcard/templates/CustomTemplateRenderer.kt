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
 * -----------------------------------------------
 * Template 1: PolaroidRenderer
 * -----------------------------------------------
 * Creates a classic "Polaroid"-style white border frame around the image.
 * - Adds equal padding on top/left/right and a thicker bottom margin.
 * - Displays device name and photo metadata (focal length, aperture, shutter, ISO)
 * centered at the bottom.
 * - Compatible with all Android versions.
 */
class PolaroidRenderer : TemplateRenderer {
    override fun render(context: Context, original: Bitmap, exif: ExifData, deviceName: String): Bitmap {
        val borderTopLeftRight = (original.width * 0.05f).toInt()
        val borderBottom = (original.height * 0.25f).toInt()
        val newWidth = original.width + (borderTopLeftRight * 2)
        val newHeight = original.height + borderTopLeftRight + borderBottom

        val result = createBitmap(newWidth, newHeight, original.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        // Center original photo inside the white frame
        val photoRect = Rect(borderTopLeftRight, borderTopLeftRight, newWidth - borderTopLeftRight, newHeight - borderBottom)
        canvas.drawBitmap(original, null, photoRect, null)

        // Copy Ultra HDR gainmap if applicable
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            handleGainmap(original, result, photoRect)
        }

        // Setup paint for title (device name) and metadata (EXIF)
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
 * -----------------------------------------------
 * Template 2: AeroBlueRenderer (API 31+ only)
 * -----------------------------------------------
 * Renders the photo with a heavily blurred and darkened version of itself in the background.
 * - Creates a modern social-media-like aesthetic.
 * - Adds a blurred background, overlays the original image, and shows text at the bottom.
 * - Text is white and left-aligned with subtle drop shadows for contrast.
 * - Timestamp is shown on the right side of the metadata line.
 */
@RequiresApi(Build.VERSION_CODES.S)
class AeroBlueRenderer : TemplateRenderer {
    override fun render(context: Context, original: Bitmap, exif: ExifData, deviceName: String): Bitmap {
        val border = (original.width * 0.02f).toInt()
        val captionHeight = (original.height * 0.15f).toInt()
        val newWidth = original.width + 2 * border
        val newHeight = original.height + border + captionHeight

        // 1. Scale original image to fill background and apply strong blur
        val bgBitmap = original.scale(newWidth, newHeight)
        val blurredBackground = blurBitmap(context, bgBitmap, 250f)

        // 2. Draw blurred background with dark overlay
        val result = createBitmap(newWidth, newHeight, original.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(blurredBackground, 0f, 0f, null)
        canvas.drawColor(Color.argb(95, 0, 0, 0)) // Dark overlay

        // 3. Draw original photo centered above blurred background
        val photoRect = Rect(border, border, newWidth - border, newHeight - captionHeight)
        canvas.drawBitmap(original, null, photoRect, null)

        // 4. Prepare shadowed white text paints
        val deviceNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = newWidth * 0.035f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textAlign = Paint.Align.LEFT
            setShadowLayer(10f, 2f, 2f, Color.argb(150, 0, 0, 0))
        }
        val metadataPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 220
            textSize = newWidth * 0.022f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
            setShadowLayer(8f, 2f, 2f, Color.argb(150, 0, 0, 0))
        }

        // 5. Draw text in bottom area
        val padding = border.toFloat()
        val textBounds = Rect()
        deviceNamePaint.getTextBounds("A", 0, 1, textBounds)
        val deviceLineHeight = textBounds.height() * 1.5f
        val baseY = photoRect.bottom + (captionHeight - deviceLineHeight * 2) / 2 + deviceLineHeight
        canvas.drawText(deviceName, padding, baseY, deviceNamePaint)

        // Draw metadata on the left of the second line
        val metadataText = "● ${exif.focalLength} | ${exif.aperture} | ${exif.shutterSpeed} | ISO${exif.iso}"
        metadataPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(metadataText, padding, baseY + deviceLineHeight, metadataPaint)

        // Draw timestamp on the right of the second line
        val formattedTimestamp = formatTimestamp(exif.timestamp)
        if (formattedTimestamp.isNotEmpty()) {
            metadataPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(formattedTimestamp, newWidth - padding, baseY + deviceLineHeight, metadataPaint)
        }

        // 6. Handle HDR gainmap
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            handleGainmap(original, result, photoRect)
        }
        return result
    }
}

/**
 * -----------------------------------------------
 * Template 3: BottomBarRenderer
 * -----------------------------------------------
 * Adds a clean white bar under the image and displays two lines of text.
 * - Suitable for social media exports or screenshot tagging.
 * - Keeps full photo visible, only expands height.
 * - Timestamp is shown on the right side of the metadata line.
 */
class BottomBarRenderer : TemplateRenderer {
    override fun render(context: Context, original: Bitmap, exif: ExifData, deviceName: String): Bitmap {
        val barHeight = (original.height * 0.15).toInt()
        val newHeight = original.height + barHeight

        // 1. Create taller bitmap with white background
        val result = createBitmap(original.width, newHeight, original.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        // 2. Draw original image at top
        canvas.drawBitmap(original, 0f, 0f, null)

        // 3. Setup left-aligned text paint
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

        // 4. Calculate Y positions and draw text
        val textBounds = Rect()
        paintDevice.getTextBounds("A", 0, 1, textBounds)
        val deviceLineHeight = textBounds.height() * 1.5f
        val baseY = original.height + (barHeight - deviceLineHeight * 2) / 2 + deviceLineHeight
        canvas.drawText(deviceName, padding, baseY, paintDevice)

        // Draw metadata on the left of the second line
        val metadataText = "${exif.focalLength} | ${exif.aperture} | ${exif.shutterSpeed} | ISO${exif.iso}"
        paintExif.textAlign = Paint.Align.LEFT
        canvas.drawText(metadataText, padding, baseY + deviceLineHeight, paintExif)

        // Draw timestamp on the right of the second line
        val formattedTimestamp = formatTimestamp(exif.timestamp)
        if (formattedTimestamp.isNotEmpty()) {
            paintExif.textAlign = Paint.Align.RIGHT
            canvas.drawText(formattedTimestamp, original.width - padding, baseY + deviceLineHeight, paintExif)
        }

        // 5. Copy HDR gainmap if present
        val photoRect = Rect(0, 0, original.width, original.height)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            handleGainmap(original, result, photoRect)
        }
        return result
    }
}

/**
 * -----------------------------------------------
 * Template 4: SunsetRenderer
 * -----------------------------------------------
 * A minimal border style that adds a small caption area under the image.
 * - Ideal for editorial-style export (blog, gallery, camera showcase).
 * - Left-aligned text and clean white background.
 * - Timestamp is shown on the right side of the metadata line.
 */
class SunsetRenderer : TemplateRenderer {
    override fun render(context: Context, original: Bitmap, exif: ExifData, deviceName: String): Bitmap {
        val border = (original.width * 0.02).toInt()
        val captionHeight = (original.height * 0.15).toInt()
        val newWidth = original.width + 2 * border
        val newHeight = original.height + border + captionHeight
        val result = createBitmap(newWidth, newHeight)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        // Draw original photo inside inset area
        val photoRect = Rect(border, border, newWidth - border, newHeight - captionHeight)
        canvas.drawBitmap(original, null, photoRect, null)

        // Text styles for device name and metadata
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

        val textBounds = Rect()
        paintDevice.getTextBounds("A", 0, 1, textBounds)
        val deviceLineHeight = textBounds.height() * 1.5f
        val baseY = photoRect.bottom + (captionHeight - deviceLineHeight * 2) / 2 + deviceLineHeight
        canvas.drawText(deviceName, padding, baseY, paintDevice)

        // Draw metadata on the left of the second line
        val metadataText = "● ${exif.focalLength} | ${exif.aperture} | ${exif.shutterSpeed} | ISO${exif.iso}"
        paintExif.textAlign = Paint.Align.LEFT
        canvas.drawText(metadataText, padding, baseY + deviceLineHeight, paintExif)

        // Draw timestamp on the right of the second line
        val formattedTimestamp = formatTimestamp(exif.timestamp)
        if (formattedTimestamp.isNotEmpty()) {
            paintExif.textAlign = Paint.Align.RIGHT
            canvas.drawText(formattedTimestamp, newWidth - padding, baseY + deviceLineHeight, paintExif)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            handleGainmap(original, result, photoRect)
        }
        return result
    }
}

// ---------------------------------------------
// Helper: Copy Gainmap (HDR Metadata)
// ---------------------------------------------

/**
 * Copies Ultra HDR gainmap from the original Bitmap to the result Bitmap.
 * - A gainmap stores a second image layer that adjusts brightness on HDR displays.
 * - This is only supported on Android 14 (API 34) and above.
 */
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

/**
 * Parses and formats EXIF date strings to user-friendly form.
 * Input: "yyyy:MM:dd HH:mm:ss" → Output: "11 July 2023 at 15:45"
 */
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
