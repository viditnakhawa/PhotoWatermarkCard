package com.viditnakhawa.photowatermarkcard.gallery

import android.app.Application
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface // --- NEW --- Add this import
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val _timelineItems = MutableStateFlow<List<TimelineItem>>(emptyList())
    val timelineItems = _timelineItems.asStateFlow()

    fun loadAutoFramedImages() {
        viewModelScope.launch(Dispatchers.IO) {
            val imageList = mutableListOf<GalleryItem>()

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_ADDED, //Fallback
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT
            )

            val selection = "${MediaStore.Images.Media.DATA} like ?"
            val selectionArgs = arrayOf("%/AutoFramed/%")
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val contentResolver = getApplication<Application>().contentResolver

            contentResolver.query(queryUri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val contentUri = ContentUris.withAppendedId(queryUri, id)

                    // 1. Try to get the timestamp directly from the file's EXIF data.
                    val exifTimestamp = getExifTimestamp(contentUri)

                    // 2. If EXIF fails, fall back to DATE_ADDED from MediaStore.
                    val finalTimestamp = if (exifTimestamp != null) {
                        exifTimestamp
                    } else {
                        val dateAdded = cursor.getLong(dateAddedColumn)
                        dateAdded * 1000
                    }

                    imageList.add(GalleryItem(uri = contentUri, dateTimestamp = finalTimestamp, width = width, height = height))
                }
            }

            val sortedList = imageList.sortedByDescending { it.dateTimestamp }

            _timelineItems.value = createTimeline(sortedList)
        }
    }

    private fun getExifTimestamp(uri: Uri): Long? {
        try {
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { inputStream ->
                val exifInterface = ExifInterface(inputStream)
                val dateString = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exifInterface.getAttribute(ExifInterface.TAG_DATETIME)

                if (dateString != null) {
                    // Parse the string into a real date object
                    val format = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
                    return format.parse(dateString)?.time
                }
            }
        } catch (e: Exception) {

            e.printStackTrace()
        }
        return null
    }

    private fun createTimeline(images: List<GalleryItem>): List<TimelineItem> {
        val timeline = mutableListOf<TimelineItem>()
        val groupedByDate = images.groupBy { getFormattedDate(it.dateTimestamp) }

        for ((date, items) in groupedByDate) {
            timeline.add(TimelineItem.HeaderItem(date))
            timeline.addAll(items.map { TimelineItem.ImageItem(it) })
        }
        return timeline
    }

    private fun getFormattedDate(timestampMillis: Long): String {
        val todayCal = Calendar.getInstance()
        val yesterdayCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }

        val itemCal = Calendar.getInstance().apply {
            timeInMillis = timestampMillis
        }

        if (itemCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
            itemCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)) {
            return "Today"
        }

        if (itemCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
            itemCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)) {
            return "Yesterday"
        }

        return SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(itemCal.time)
    }

    fun deleteImage(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = getApplication<Application>().contentResolver
                contentResolver.delete(uri, null, null)
                loadAutoFramedImages()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteImages(uris: Set<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = getApplication<Application>().contentResolver
                for (uri in uris) {
                    contentResolver.delete(uri, null, null)
                }
                loadAutoFramedImages()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}