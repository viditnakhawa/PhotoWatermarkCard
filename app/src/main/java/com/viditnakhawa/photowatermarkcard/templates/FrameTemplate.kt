package com.viditnakhawa.photowatermarkcard.templates

import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Represents a single frame template available in the app.
 * It directly links a template's identity with its rendering logic.
 *
 * @param id A unique identifier for the template.
 * @param name The user-facing name for the template.
 * @param renderer The renderer instance responsible for drawing this template.
 */
data class FrameTemplate(
    val id: String,
    val name: String,
    val renderer: TemplateRenderer
)

/**
 * A repository that holds all available frame templates.
 * This object is the single source of truth for what templates can be selected in the UI.
 */
object TemplateRepository {
    private val polaroidRenderer = PolaroidRenderer()
    private val bottomBarRenderer = BottomBarRenderer()
    private val sunsetRenderer = SunsetRenderer()

    // This renderer requires API 31+ for RenderEffect.
    @RequiresApi(Build.VERSION_CODES.S)
    private val aeroBlueRenderer = AeroBlueRenderer()

    /**
     * The list of all available templates.
     * It's built dynamically to include modern templates only on compatible devices.
     */
    val templates: List<FrameTemplate> = buildList {
        add(FrameTemplate("polaroid", "Polaroid", polaroidRenderer))
        add(FrameTemplate("sunset", "Sunset", sunsetRenderer))
        add(FrameTemplate("bottom_bar", "Bottom Bar", bottomBarRenderer))


        //RenderEffect-based template only if the device supports it.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(FrameTemplate("aero_blue", "Aero Blue", aeroBlueRenderer))
        }
    }

    /**
     * Finds a template by its unique ID.
     *
     * @param id The ID of the template to find.
     * @return The found FrameTemplate, or the default "Polaroid" template as a fallback.
     */
    fun findById(id: String?): FrameTemplate? {
        // Find the template by its ID. If not found, default to the Polaroid template.
        return templates.find { it.id == id } ?: templates.find { it.id == "polaroid" }
    }
}
