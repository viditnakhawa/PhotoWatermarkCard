package com.viditnakhawa.photowatermarkcard.utils

import android.content.Context
import androidx.annotation.DrawableRes

object LogoUtils {

    /**
     * Finds the drawable resource ID for a given manufacturer's logo.
     *
     * @param context The application context.
     * @param manufacturer The name of the manufacturer (e.g., "Google", "Samsung").
     * @return The resource ID of the logo, or 0 if not found.
     */
    @DrawableRes
    fun getLogoResource(context: Context, manufacturer: String): Int {
        val resourceName = manufacturer.lowercase().replace(" ", "_") + "_logo"

        return context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    }


    /**
     * Finds the resource ID for a manufacturer's icon, falling back to the main logo.
     * @return The resource ID, or 0 if neither are found.
     */
    @DrawableRes
    fun getIconOrLogoResource(context: Context, manufacturer: String): Int {
        val iconResourceName = manufacturer.lowercase().replace(" ", "_") + "_icon_logo"
        val iconResId = context.resources.getIdentifier(iconResourceName, "drawable", context.packageName)

        if (iconResId != 0) {
            return iconResId
        }

        // 2. If the icon isn't found, fall back to the original logo function
        return getLogoResource(context, manufacturer)
    }
}