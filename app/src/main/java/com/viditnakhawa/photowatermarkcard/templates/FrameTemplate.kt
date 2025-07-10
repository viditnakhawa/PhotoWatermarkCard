package com.viditnakhawa.photowatermarkcard.templates

import androidx.annotation.FontRes
import androidx.compose.ui.graphics.Color

data class FrameTemplate(
    val id: String,
    val name: String,
    val frameColor: Color,
    val deviceNameTextColor: Color,
    val metadataTextColor: Color,
    @FontRes val deviceNameFontResId: Int?
)

object TemplateRepository {
    val templates = listOf(
        FrameTemplate(
            id = "classic_white",
            name = "Classic White",
            frameColor = Color.White,
            deviceNameTextColor = Color.Black,
            metadataTextColor = Color.DarkGray,
            deviceNameFontResId = null
        ),
        FrameTemplate(
            id = "classic_black",
            name = "Classic Black",
            frameColor = Color.Black,
            deviceNameTextColor = Color.White,
            metadataTextColor = Color.LightGray,
            deviceNameFontResId = null
        ),
        FrameTemplate(
            id = "twilight_blue",
            name = "Twilight Blue",
            frameColor = Color(0xFF0A192F), // Dark blue
            deviceNameTextColor = Color.White.copy(alpha = 0.9f),
            metadataTextColor = Color.White.copy(alpha = 0.7f),
            deviceNameFontResId = null
        ),
        FrameTemplate(
            id = "minimalist_light",
            name = "Minimalist Light",
            frameColor = Color.White,
            deviceNameTextColor = Color(0xFF333333),
            metadataTextColor = Color(0xFF757575),
            deviceNameFontResId = null
        )
    )

    fun findById(id: String?): FrameTemplate {
        return templates.find { it.id == id } ?: templates.first()
    }
}
