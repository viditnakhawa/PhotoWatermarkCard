package com.viditnakhawa.photowatermarkcard.gallery

import android.net.Uri

data class GalleryItem(
    val uri: Uri,
    val dateTimestamp: Long,
    val width: Int,
    val height: Int
)

//Used to represent either an image or a date header in list
sealed class TimelineItem {
    data class ImageItem(val galleryItem: GalleryItem) : TimelineItem()
    data class HeaderItem(val date: String) : TimelineItem()
}
