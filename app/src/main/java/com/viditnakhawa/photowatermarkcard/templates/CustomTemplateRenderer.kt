package com.viditnakhawa.photowatermarkcard.templates

import android.content.Context
import android.graphics.*
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import blurBitmap
import com.viditnakhawa.photowatermarkcard.ExifData
import com.viditnakhawa.photowatermarkcard.R // Import R class to access resources
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

/**
 * -----------------------------------------------
 * Template 1: PolaroidRenderer
 * -----------------------------------------------
 * Creates a classic "Polaroid"-style white border frame around the image.
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

        val photoRect = Rect(borderTopLeftRight, borderTopLeftRight, newWidth - borderTopLeftRight, newHeight - borderBottom)
        canvas.drawBitmap(original, null, photoRect, null)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            handleGainmap(original, result, photoRect)
        }

        // --- CUSTOM FONT HANDLING ---
        val outfitFont = ResourcesCompat.getFont(context, R.font.outfit_variablefont_wght)
        val boldTypeface = Typeface.create(outfitFont, 800, false) // Bolder weight
        val regularTypeface = Typeface.create(outfitFont, 400, false)

        val deviceNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = newWidth * 0.0495f // Increased by 10%
            typeface = boldTypeface
            textAlign = Paint.Align.CENTER
        }
        val metadataPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = newWidth * 0.0275f // Increased by 10%
            typeface = regularTypeface
            textAlign = Paint.Align.CENTER
        }

        val textCenterX = canvas.width / 2f
        val deviceNameY = photoRect.bottom + (borderBottom * 0.45f)
        val metadataY = deviceNameY + (borderBottom * 0.3f)
        canvas.drawText(deviceName, textCenterX, deviceNameY, deviceNamePaint)
        val metadataText = "${exif.focalLength}mm | ${exif.aperture} | ${exif.shutterSpeed} | ISO ${exif.iso}"
        canvas.drawText(metadataText, textCenterX, metadataY, metadataPaint)

        return result
    }
}

/**
 * -----------------------------------------------
 * Template 2: AeroBlueRenderer (API 31+ only)
 * -----------------------------------------------
 * Renders the photo on a heavily blurred background with dynamic text color.
 */
@RequiresApi(Build.VERSION_CODES.S)
class AeroBlueRenderer : TemplateRenderer {
    override fun render(context: Context, original: Bitmap, exif: ExifData, deviceName: String): Bitmap {
        val border = (original.width * 0.02f).toInt()
        val captionHeight = (original.height * 0.15f).toInt()
        val newWidth = original.width + 2 * border
        val newHeight = original.height + border + captionHeight

        val bgBitmap = original.scale(newWidth, newHeight)
        val blurredBackground = blurBitmap(context, bgBitmap, 250f)

        val result = createBitmap(newWidth, newHeight, original.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(blurredBackground, 0f, 0f, null)
        canvas.drawColor(Color.argb(95, 0, 0, 0))

        val photoRect = Rect(border, border, newWidth - border, newHeight - captionHeight)
        canvas.drawBitmap(original, null, photoRect, null)

        // --- DYNAMIC TEXT COLOR LOGIC ---
        val sampleX = border + (newWidth * 0.1f).toInt()
        val sampleY = photoRect.bottom + (captionHeight / 2)
        val bgColor = result.getPixel(sampleX, sampleY)
        // Default to white text unless the background is very light
        val useBlackText = isColorDark(bgColor) && Color.luminance(bgColor) > 0.8f
        val textColor = if (useBlackText) Color.BLACK else Color.WHITE
        val shadowColor = if (useBlackText) Color.argb(100, 255, 255, 255) else Color.argb(150, 0, 0, 0)


        // --- CUSTOM FONT HANDLING ---
        val outfitFont = ResourcesCompat.getFont(context, R.font.outfit_variablefont_wght)
        val boldTypeface = Typeface.create(outfitFont, 800, false) // Bolder weight
        val regularTypeface = Typeface.create(outfitFont, 400, false)

        val deviceNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = newWidth * 0.0385f // Increased by 10%
            typeface = boldTypeface
            textAlign = Paint.Align.LEFT
            setShadowLayer(10f, 2f, 2f, shadowColor)
        }
        val metadataPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            alpha = if(useBlackText) 255 else 220
            textSize = newWidth * 0.0242f // Increased by 10%
            typeface = regularTypeface
            textAlign = Paint.Align.LEFT
            setShadowLayer(8f, 2f, 2f, shadowColor)
        }

        val padding = border.toFloat()
        val textBounds = Rect()
        deviceNamePaint.getTextBounds("A", 0, 1, textBounds)
        val deviceLineHeight = textBounds.height() * 1.5f
        val baseY = photoRect.bottom + (captionHeight - (deviceLineHeight * 3)) / 2 + deviceLineHeight // Adjusted for 3 lines
        canvas.drawText(deviceName, padding, baseY, deviceNamePaint)

        val metadataText = "● ${exif.focalLength}mm | ${exif.aperture} | ${exif.shutterSpeed} | ISO ${exif.iso}"
        canvas.drawText(metadataText, padding, baseY + deviceLineHeight, metadataPaint)

        val formattedTimestamp = formatTimestamp(exif.timestamp)
        if (formattedTimestamp.isNotEmpty()) {
            canvas.drawText(formattedTimestamp, padding, baseY + deviceLineHeight * 2, metadataPaint)
        }

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
 * Adds a clean white bar under the image with text.
 */
class BottomBarRenderer : TemplateRenderer {
    override fun render(context: Context, original: Bitmap, exif: ExifData, deviceName: String): Bitmap {
        val barHeight = (original.height * 0.15).toInt()
        val newHeight = original.height + barHeight

        val result = createBitmap(original.width, newHeight, original.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(original, 0f, 0f, null)

        // --- CUSTOM FONT HANDLING ---
        val outfitFont = ResourcesCompat.getFont(context, R.font.outfit_variablefont_wght)
        val boldTypeface = Typeface.create(outfitFont, 800, false) // Bolder weight
        val regularTypeface = Typeface.create(outfitFont, 400, false)

        val padding = original.width * 0.04f
        val paintDevice = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = original.width * 0.0385f // Increased by 10%
            typeface = boldTypeface
        }
        val paintExif = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = original.width * 0.0242f // Increased by 10%
            typeface = regularTypeface
        }

        val textBounds = Rect()
        paintDevice.getTextBounds("A", 0, 1, textBounds)
        val deviceLineHeight = textBounds.height() * 1.5f
        val baseY = original.height + (barHeight - (deviceLineHeight * 3)) / 2 + deviceLineHeight // Adjusted for 3 lines
        canvas.drawText(deviceName, padding, baseY, paintDevice)

        val metadataText = "${exif.focalLength}mm | ${exif.aperture} | ${exif.shutterSpeed} | ISO ${exif.iso}"
        paintExif.textAlign = Paint.Align.LEFT
        canvas.drawText(metadataText, padding, baseY + deviceLineHeight, paintExif)

        val formattedTimestamp = formatTimestamp(exif.timestamp)
        if (formattedTimestamp.isNotEmpty()) {
            paintExif.textAlign = Paint.Align.LEFT
            canvas.drawText(formattedTimestamp, padding, baseY + deviceLineHeight * 2, paintExif)
        }

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
 * A minimal border style with a caption area.
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

        val photoRect = Rect(border, border, newWidth - border, newHeight - captionHeight)
        canvas.drawBitmap(original, null, photoRect, null)

        // --- CUSTOM FONT HANDLING ---
        val outfitFont = ResourcesCompat.getFont(context, R.font.outfit_variablefont_wght)
        val boldTypeface = Typeface.create(outfitFont, 800, false) // Bolder weight
        val regularTypeface = Typeface.create(outfitFont, 400, false)

        val padding = border.toFloat()
        val paintDevice = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = newWidth * 0.0385f // Increased by 10%
            typeface = boldTypeface
            textAlign = Paint.Align.LEFT
        }
        val paintExif = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = newWidth * 0.0242f // Increased by 10%
            typeface = regularTypeface
            textAlign = Paint.Align.LEFT
        }

        val textBounds = Rect()
        paintDevice.getTextBounds("A", 0, 1, textBounds)
        val deviceLineHeight = textBounds.height() * 1.5f
        val baseY = photoRect.bottom + (captionHeight - (deviceLineHeight * 3)) / 2 + deviceLineHeight // Adjusted for 3 lines
        canvas.drawText(deviceName, padding, baseY, paintDevice)

        val metadataText = "● ${exif.focalLength}mm | ${exif.aperture} | ${exif.shutterSpeed} | ISO ${exif.iso}"
        canvas.drawText(metadataText, padding, baseY + deviceLineHeight, paintExif)

        val formattedTimestamp = formatTimestamp(exif.timestamp)
        if (formattedTimestamp.isNotEmpty()) {
            canvas.drawText(formattedTimestamp, padding, baseY + deviceLineHeight * 2, paintExif)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            handleGainmap(original, result, photoRect)
        }
        return result
    }
}

// ---------------------------------------------
// Helper Functions
// ---------------------------------------------

/**
 * Checks if a color is perceived as "dark" by calculating its luminance.
 * @return True if the color is dark, false if it is light.
 */
private fun isColorDark(@ColorInt color: Int): Boolean {
    val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
    return darkness >= 0.5
}

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
