package com.viditnakhawa.photowatermarkcard.templates

import android.R.attr.padding
import android.content.Context
import android.graphics.*
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import blurBitmap
import com.viditnakhawa.photowatermarkcard.ExifData
import com.viditnakhawa.photowatermarkcard.R
import com.viditnakhawa.photowatermarkcard.utils.LogoUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.get
import androidx.palette.graphics.Palette
import androidx.core.graphics.toColorInt


/**
 * -----------------------------------------------
 * Template 1: PolaroidRenderer
 * -----------------------------------------------
 * Creates a classic "Polaroid"-style white border frame around the image.
 */
class PolaroidRenderer : TemplateRenderer {
    override fun render(context: Context, original: Bitmap, exif: ExifData, deviceName: String, manufacturer: String, model: String): Bitmap {
        val shorterEdge = min(original.width, original.height)
        val borderTopLeftRight = (shorterEdge * 0.05f).toInt()
        val borderBottom = (shorterEdge * 0.25f).toInt()
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

        val outfitFont = ResourcesCompat.getFont(context, R.font.outfit_variablefont_wght)
        val boldTypeface = Typeface.create(outfitFont, 800, false)
        val regularTypeface = Typeface.create(outfitFont, 400, false)

        // --- LOGO & TEXT DRAWING LOGIC ---
        val logoResId = LogoUtils.getLogoResource(context, manufacturer)

        if (logoResId != 0) {
            // --- DRAW LOGO & MODEL NAME ---
            val logoBitmap = BitmapFactory.decodeResource(context.resources, logoResId)

            val modelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = shorterEdge * 0.035f
                typeface = boldTypeface
                textAlign = Paint.Align.CENTER
            }
            val exifPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.DKGRAY
                textSize = shorterEdge * 0.03f
                typeface = regularTypeface
                textAlign = Paint.Align.CENTER
            }

            // --- UNIFORM LOGO SIZING ---
            val maxLogoHeight = shorterEdge * 0.09f
            val aspectRatio = logoBitmap.width.toFloat() / logoBitmap.height.toFloat()
            val scaledHeight = maxLogoHeight
            val scaledWidth = scaledHeight * aspectRatio

            // --- VERTICAL CENTERING LOGIC ---
            val lineSpacing = shorterEdge * 0.02f // Consistent spacing between elements
            val totalContentHeight = scaledHeight + lineSpacing + modelPaint.textSize + lineSpacing + exifPaint.textSize
            var currentY = photoRect.bottom + (borderBottom - totalContentHeight) / 2

            // Draw Logo
            val logoRect = RectF(
                (newWidth - scaledWidth) / 2,
                currentY,
                (newWidth + scaledWidth) / 2,
                currentY + scaledHeight
            )
            canvas.drawBitmap(logoBitmap, null, logoRect, null)
            currentY = logoRect.bottom // Update current Y position

            // Draw Model Name
            currentY += lineSpacing + modelPaint.textSize
            canvas.drawText(model, newWidth / 2f, currentY, modelPaint)

            // Draw EXIF Data
            currentY += lineSpacing + exifPaint.textSize
            val exifText = "${exif.focalLength}mm | ${exif.aperture} | ${exif.shutterSpeed} | ISO ${exif.iso}"
            canvas.drawText(exifText, newWidth / 2f, currentY, exifPaint)

        } else {
            // --- FALLBACK TO TEXT-ONLY ---
            val deviceNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = shorterEdge * 0.06f
                typeface = boldTypeface
                textAlign = Paint.Align.CENTER
            }
            val metadataPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.DKGRAY
                textSize = shorterEdge * 0.035f
                typeface = regularTypeface
                textAlign = Paint.Align.CENTER
            }

            val textCenterX = canvas.width / 2f
            val deviceNameY = photoRect.bottom + (borderBottom * 0.45f)
            val metadataY = deviceNameY + (borderBottom * 0.3f)
            canvas.drawText(deviceName, textCenterX, deviceNameY, deviceNamePaint)
            val metadataText = "${exif.focalLength}mm | ${exif.aperture} | ${exif.shutterSpeed} | ISO ${exif.iso}"
            canvas.drawText(metadataText, textCenterX, metadataY, metadataPaint)
        }

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
    override fun render(context: Context, original: Bitmap, exif: ExifData, deviceName: String, manufacturer: String, model: String): Bitmap {
        val shorterEdge = min(original.width, original.height)
        val border = (shorterEdge * 0.04f).toInt()
        val captionHeight = (shorterEdge * 0.20f).toInt()
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
        val bgColor = result[sampleX, sampleY]
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
            textSize = shorterEdge * 0.050f
            typeface = boldTypeface
            textAlign = Paint.Align.LEFT
            setShadowLayer(10f, 2f, 2f, shadowColor)
        }
        val metadataPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            alpha = if (useBlackText) 255 else 220
            textSize = shorterEdge * 0.032f
            typeface = regularTypeface
            textAlign = Paint.Align.LEFT
            setShadowLayer(8f, 2f, 2f, shadowColor)
        }

        val padding = border.toFloat()

        // Get font metrics for both text blocks
        val deviceMetrics = deviceNamePaint.fontMetrics
        val metadataMetrics = metadataPaint.fontMetrics

        val deviceLineHeight = deviceMetrics.descent - deviceMetrics.ascent
        val metadataLineHeight = metadataMetrics.descent - metadataMetrics.ascent

        val totalHeight = deviceLineHeight + 2 * metadataLineHeight
        val captionTop = photoRect.bottom
        val captionCenterY = captionTop + (captionHeight / 2f)

        // Align all 3 lines so they're vertically centered
        val deviceY = captionCenterY - (totalHeight / 2f) - deviceMetrics.ascent
        val metadata1Y = deviceY + metadataLineHeight
        val metadata2Y = metadata1Y + metadataLineHeight

        canvas.drawText(deviceName, padding, deviceY, deviceNamePaint)

        val metadataText = "● ${exif.focalLength}mm | ${exif.aperture} | ${exif.shutterSpeed} | ISO ${exif.iso}"
        canvas.drawText(metadataText, padding, metadata1Y, metadataPaint)

        val formattedTimestamp = formatTimestamp(exif.timestamp)
        if (formattedTimestamp.isNotEmpty()) {
            canvas.drawText(formattedTimestamp, padding, metadata2Y, metadataPaint)
        }


        // --- SAMPLE SIX COLORS FROM DIFFERENT REGIONS ---
        val midColor = result[sampleX, sampleY]
        val palette = Palette.from(original).maximumColorCount(12).generate()

        // Filter out near-white and near-black colors
        fun isVisibleColor(color: Int): Boolean {
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            return !((r > 240 && g > 240 && b > 240) || (r < 20 && g < 20 && b < 20))
        }

        // Extract and clean dominant swatches
        val dominantColors = palette.swatches
            .map { it.rgb }
            .filter { isVisibleColor(it) }
            .distinct()
            .take(6)

        // Fallback if fewer than 6
        val fallbackColor = Color.GRAY
        val colorsToDraw = (dominantColors + List(6) { fallbackColor }).take(6)

        // --- Drawing Setup ---
        val circleRadius = shorterEdge * 0.018f
        val spacingX = circleRadius * 3f
        val spacingY = circleRadius * 2.8f

        val circlesPerRow = 3
        val totalRowWidth = (circlesPerRow - 1) * spacingX
        val startX = newWidth - border - totalRowWidth - circleRadius // right-aligned

        // Center the two rows in the caption area
        val startY = captionCenterY - (spacingY / 2f)

        // Paints
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = circleRadius * 0.1f
            color = if (isColorDark(midColor)) Color.WHITE else Color.BLACK
        }

        // Draw the circles
        colorsToDraw.forEachIndexed { index, color ->
            val row = index / circlesPerRow
            val col = index % circlesPerRow
            val cx = startX + col * spacingX
            val cy = startY + row * spacingY
            fillPaint.color = color
            canvas.drawCircle(cx, cy, circleRadius, fillPaint)
            canvas.drawCircle(cx, cy, circleRadius, strokePaint) // Contrast border
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
    override fun render(
        context: Context,
        original: Bitmap,
        exif: ExifData,
        deviceName: String,
        manufacturer: String,
        model: String
    ): Bitmap {
        val shorterEdge = min(original.width, original.height)
        val barHeight = (shorterEdge * 0.15).toInt()
        val newHeight = original.height + barHeight

        // --- 1. Create the final canvas FIRST ---
        val result =
            createBitmap(original.width, newHeight, original.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // --- 2. Draw the background and original image ---
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(original, 0f, 0f, null)

        // --- 3. Prepare fonts and paints ---
        val outfitFont = ResourcesCompat.getFont(context, R.font.outfit_variablefont_wght)
        val boldTypeface = Typeface.create(outfitFont, 800, false)
        val regularTypeface = Typeface.create(outfitFont, 400, false)

        val padding = shorterEdge * 0.04f
        val paintDevice = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = shorterEdge * 0.045f
            typeface = boldTypeface
        }
        val paintExif = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = shorterEdge * 0.028f
            typeface = regularTypeface
        }

        // --- 4. Draw the text on the left ---
        val textBounds = Rect()
        paintDevice.getTextBounds("A", 0, 1, textBounds)
        val deviceLineHeight = textBounds.height() * 1.25f
        val baseY = original.height + (barHeight - (deviceLineHeight * 3)) / 2 + deviceLineHeight
        canvas.drawText(deviceName, padding, baseY, paintDevice)

        val metadataText =
            "${exif.focalLength}mm | ${exif.aperture} | ${exif.shutterSpeed} | ISO ${exif.iso}"
        paintExif.textAlign = Paint.Align.LEFT
        canvas.drawText(metadataText, padding, baseY + deviceLineHeight, paintExif)

        val formattedTimestamp = formatTimestamp(exif.timestamp)
        if (formattedTimestamp.isNotEmpty()) {
            paintExif.textAlign = Paint.Align.LEFT
            canvas.drawText(
                formattedTimestamp,
                padding,
                baseY + deviceLineHeight * 2,
                paintExif
            )
        }

        // --- 5. Draw the icon on the right ---
        val iconResId = LogoUtils.getIconOrLogoResource(context, manufacturer)
        if (iconResId != 0) {
            val iconBitmap = BitmapFactory.decodeResource(context.resources, iconResId)

            val maxIconHeight = barHeight * 0.6f
            val aspectRatio = iconBitmap.width.toFloat() / iconBitmap.height.toFloat()
            val scaledHeight = maxIconHeight
            val scaledWidth = scaledHeight * aspectRatio

            val rightPadding = shorterEdge * 0.04f
            val iconTop = original.height + (barHeight - scaledHeight) / 2f
            val iconLeft = original.width - rightPadding - scaledWidth
            val iconRect = RectF(iconLeft, iconTop, iconLeft + scaledWidth, iconTop + scaledHeight)

            canvas.drawBitmap(iconBitmap, null, iconRect, null)
        }

        // --- 6. Handle Gainmap and return the final result ---
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
    override fun render(context: Context, original: Bitmap, exif: ExifData, deviceName: String, manufacturer: String, model: String): Bitmap {
        val shorterEdge = min(original.width, original.height)
        val border = (shorterEdge * 0.03).toInt()
        val captionHeight = (shorterEdge * 0.15f).toInt()
        val newWidth = original.width + 2 * border
        val newHeight = original.height + border + captionHeight

        val photoRect = Rect(border, border, newWidth - border, newHeight - captionHeight)
        val result = createBitmap(newWidth, newHeight)
        val canvas = Canvas(result)

        val sharedPrefs = context.getSharedPreferences("AutoFramePrefs", Context.MODE_PRIVATE)
        val useGradient = sharedPrefs.getBoolean("sunset_gradient_enabled", true)

        // --- Gradient Background ---
        val sampleHeight = (original.height * 0.05f).toInt()
        val topRegionBitmap = Bitmap.createBitmap(original, 0, 0, original.width, sampleHeight)
        val bottomRegionBitmap = Bitmap.createBitmap(original, 0, original.height - sampleHeight, original.width, sampleHeight)

        val midColor: Int

        if (useGradient) {
            val topColor = topRegionBitmap.scale(1, 1, false)[0, 0]
            val bottomColor = bottomRegionBitmap.scale(1, 1, false)[0, 0]

            val imageGradient = LinearGradient(
                0f, 0f,
                0f, newHeight.toFloat(),
                topColor, bottomColor,
                Shader.TileMode.CLAMP
            )
            val gradientPaint = Paint().apply { shader = imageGradient }
            canvas.drawRect(0f, 0f, newWidth.toFloat(), newHeight.toFloat(), gradientPaint)

            // Sample midpoint color
            val midX = newWidth / 2f
            val midY = photoRect.bottom + (captionHeight / 2f)
            val bgPaint = Paint().apply { shader = imageGradient }
            val bgBitmapForSampling = createBitmap(1, 1)
            val bgCanvas = Canvas(bgBitmapForSampling)
            bgCanvas.translate(-midX, -midY)
            bgCanvas.drawRect(0f, 0f, newWidth.toFloat(), newHeight.toFloat(), bgPaint)
            midColor = bgBitmapForSampling[0, 0]
        } else {
            canvas.drawColor(Color.WHITE)
            midColor = Color.WHITE
        }

        canvas.drawBitmap(original, null, photoRect, null)

        // --- Text Color Logic ---
        val textColor = if (isColorDark(midColor)) Color.WHITE else Color.BLACK
        val subTextColor = if (isColorDark(midColor)) Color.LTGRAY else Color.DKGRAY

        val outfitFont = ResourcesCompat.getFont(context, R.font.outfit_variablefont_wght)
        val boldTypeface = Typeface.create(outfitFont, 800, false)
        val regularTypeface = Typeface.create(outfitFont, 400, false)
        val padding = (shorterEdge * 0.04f) + border

        val leftPaintDevice = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = shorterEdge * 0.050f
            typeface = boldTypeface
            textAlign = Paint.Align.LEFT
        }
        val leftPaintExif = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = subTextColor
            textSize = shorterEdge * 0.032f
            typeface = regularTypeface
            textAlign = Paint.Align.LEFT
        }
        val rightAlignPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = subTextColor
            textSize = shorterEdge * 0.032f
            typeface = regularTypeface
            textAlign = Paint.Align.RIGHT
        }

        // --- Font Metrics Based Centering ---
        val deviceMetrics = leftPaintDevice.fontMetrics
        val exifMetrics = leftPaintExif.fontMetrics

        val deviceLineHeight = deviceMetrics.descent - deviceMetrics.ascent
        val exifLineHeight = exifMetrics.descent - exifMetrics.ascent
        val totalTextHeight = deviceLineHeight + exifLineHeight

        val captionTop = photoRect.bottom
        val captionCenterY = captionTop + (captionHeight / 2f)

        val deviceBaselineY = captionCenterY - (totalTextHeight / 2f) - deviceMetrics.ascent
        val exifBaselineY = deviceBaselineY + exifLineHeight

        canvas.drawText(deviceName, padding, deviceBaselineY, leftPaintDevice)
        val metadataText = "● ${exif.focalLength}mm | ${exif.aperture} | ${exif.shutterSpeed} | ISO ${exif.iso}"
        canvas.drawText(metadataText, padding, exifBaselineY, leftPaintExif)

        // --- Right Aligned Date + Location ---
        val lat = formatGpsCoordinate(exif.gpsLatitude, exif.gpsLatitudeRef)
        val lon = formatGpsCoordinate(exif.gpsLongitude, exif.gpsLongitudeRef)

        var dateYear: String? = null
        var dateMonthDay: String? = null
        try {
            val parser = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
            val date = parser.parse(exif.timestamp)
            if (date != null) {
                dateYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(date)
                dateMonthDay = SimpleDateFormat("MM. dd", Locale.getDefault()).format(date)
            }
        } catch (e: Exception) {
            // No-op
        }

        if (dateYear != null && dateMonthDay != null) {
            canvas.drawText(dateYear, newWidth - padding, deviceBaselineY, rightAlignPaint)
            canvas.drawText(dateMonthDay, newWidth - padding, exifBaselineY, rightAlignPaint)
        }

        if (lat != null && lon != null) {
            val dateWidth = rightAlignPaint.measureText(dateMonthDay ?: "00. 00")
            val internalPadding = shorterEdge * 0.03f
            val gpsX = newWidth - padding - dateWidth - internalPadding
            canvas.drawText(lat, gpsX, deviceBaselineY, rightAlignPaint)
            canvas.drawText(lon, gpsX, exifBaselineY, rightAlignPaint)
        }

        // Gainmap (Ultra HDR)
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

/**
 * Converts raw EXIF GPS coordinate (D/M/S format) to a formatted decimal string.
 */
private fun formatGpsCoordinate(coordinate: String?, reference: String?): String? {
    if (coordinate == null || reference == null) return null
    try {
        val parts = coordinate.split(",").map { it.split("/") }
        val degrees = parts[0][0].toDouble() / parts[0][1].toDouble()
        val minutes = parts[1][0].toDouble() / parts[1][1].toDouble()
        val seconds = parts[2][0].toDouble() / parts[2][1].toDouble()
        val decimal = degrees + (minutes / 60.0) + (seconds / 3600.0)
        return String.format(Locale.US, "%.3f%s", decimal, reference)
    } catch (e: Exception) {
        return null // Return null if parsing fails
    }
}