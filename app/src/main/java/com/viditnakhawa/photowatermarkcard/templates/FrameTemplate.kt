package com.viditnakhawa.photowatermarkcard.templates

import androidx.annotation.FontRes
import androidx.compose.ui.graphics.Color

/**
 * Represents the properties of a single frame template.
 *
 * @param id A unique identifier for the template.
 * @param name The user-facing name for the template.
 * @param frameColor The primary background color of the frame.
 * @param deviceNameTextColor The color of the device name text.
 * @param metadataTextColor The color of the EXIF metadata text.
 * @param deviceNameFontResId Optional custom font for the device name.
 * @param type The type of template, which will determine the processing logic.
 */
data class FrameTemplate(
    val id: String,
    val name: String,
    val frameColor: Color,
    val deviceNameTextColor: Color,
    val metadataTextColor: Color,
    @FontRes val deviceNameFontResId: Int?,
    val type: TemplateType = TemplateType.STANDARD_FRAME // Default type
)

/**
 * Defines the different kinds of template logic.
 * This will help in the FrameUtils to decide how to process the image.
 */
enum class TemplateType {
    STANDARD_FRAME,
    BLURRED_OVERLAY,
    BOTTOM_BAR_ONLY
}

object TemplateRepository {
    val templates = listOf(
        FrameTemplate(
            id = "pixel_daylight",
            name = "Pixel Daylight",
            frameColor = Color.White,
            deviceNameTextColor = Color(0xFF333333),
            metadataTextColor = Color(0xFF757575),
            deviceNameFontResId = null,
            type = TemplateType.STANDARD_FRAME
        ),
        FrameTemplate(
            id = "blurred_overlay",
            name = "Blurred Overlay",
            frameColor = Color.Transparent,
            deviceNameTextColor = Color.White.copy(alpha = 0.9f),
            metadataTextColor = Color.White.copy(alpha = 0.7f),
            deviceNameFontResId = null,
            type = TemplateType.BLURRED_OVERLAY
        ),
        FrameTemplate(
            id = "classic_bottom_bar",
            name = "Classic Bottom Bar",
            frameColor = Color.White,
            deviceNameTextColor = Color.Black,
            metadataTextColor = Color.DarkGray,
            deviceNameFontResId = null,
            type = TemplateType.BOTTOM_BAR_ONLY
        ),
        FrameTemplate(
            id = "pixel_sunset",
            name = "Pixel Sunset",
            frameColor = Color.White,
            deviceNameTextColor = Color.Black,
            metadataTextColor = Color(0xFF555555),
            deviceNameFontResId = null,
            type = TemplateType.STANDARD_FRAME
        )
    )

    fun findById(id: String?): FrameTemplate {
        return templates.find { it.id == id } ?: templates.first()
    }
}