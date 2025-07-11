package com.viditnakhawa.photowatermarkcard.templates

import android.content.Context
import android.graphics.Bitmap
import com.viditnakhawa.photowatermarkcard.ExifData

/**
 * An interface for rendering a specific frame template.
 * Each template style will have its own implementation of this interface.
 */
interface TemplateRenderer {
    fun render(
        context: Context,
        original: Bitmap,
        exif: ExifData,
        deviceName: String
    ): Bitmap
}