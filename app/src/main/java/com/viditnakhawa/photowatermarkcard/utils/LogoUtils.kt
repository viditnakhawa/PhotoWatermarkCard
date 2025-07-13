package com.viditnakhawa.photowatermarkcard.utils

import android.content.Context
import androidx.annotation.DrawableRes
import com.viditnakhawa.photowatermarkcard.R

object LogoUtils {

    /**
     * Finds the drawable resource ID for a given manufacturer's logo using a safe and efficient `when` block.
     *
     * @param manufacturer The name of the manufacturer (e.g., "Google", "Samsung").
     * @return The resource ID of the logo, or 0 if not found.
     */
    @DrawableRes
    fun getLogoResource(context: Context, manufacturer: String): Int {
        return when (manufacturer.lowercase()) {
            "google" -> R.drawable.google_logo
            "oneplus" -> R.drawable.oneplus_logo
            "samsung" -> R.drawable.samsung_logo
            "motorola" -> R.drawable.motorola_logo
            "realme" -> R.drawable.realme_logo
            "poco" -> R.drawable.poco_logo
            "iqoo" -> R.drawable.iqoo_logo
            "oppo" -> R.drawable.oppo_logo
            "redmi" -> R.drawable.redmi_logo
            "vivo" -> R.drawable.vivo_logo
            "xiaomi" -> R.drawable.xiaomi_logo
            "cmf" -> R.drawable.cmf_logo
            "nothing" -> R.drawable.nothing_logo
            else -> R.drawable.android_icon
        }
    }


    /**
     * Finds the resource ID for a manufacturer's icon, falling back to the main logo.
     * @return The resource ID, or 0 if neither are found.
     */
    @DrawableRes
    fun getIconOrLogoResource(context: Context, manufacturer: String): Int {
        val iconResId = when (manufacturer.lowercase()) {
            "google" -> R.drawable.google_icon_logo
            "oneplus" -> R.drawable.oneplus_icon_logo
            "motorola" -> R.drawable.motorola_icon_logo
            "samsung" -> R.drawable.samsung_logo
            "realme" -> R.drawable.realme_logo
            "poco" -> R.drawable.poco_logo
            "iqoo" -> R.drawable.iqoo_logo
            "oppo" -> R.drawable.oppo_logo
            "redmi" -> R.drawable.redmi_logo
            "vivo" -> R.drawable.vivo_logo
            "xiaomi" -> R.drawable.xiaomi_icon_logo
            "cmf" -> R.drawable.cmf_logo
            "nothing" -> R.drawable.nothing_logo
            else -> R.drawable.android_icon
        }

        if (iconResId != 0) {
            return iconResId
        }

        return getLogoResource(context, manufacturer)
    }
}