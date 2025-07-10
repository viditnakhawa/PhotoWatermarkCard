package com.viditnakhawa.photowatermarkcard.gallery

import android.app.Application
import android.content.ContentUris
import android.provider.MediaStore
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
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.WIDTH, // <-- Request width
                MediaStore.Images.Media.HEIGHT // <-- Request height
            )

            val selection = "${MediaStore.Images.Media.DATA} like ?"
            val selectionArgs = arrayOf("%/AutoFramed/%")
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val contentResolver = getApplication<Application>().contentResolver

            contentResolver.query(queryUri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val contentUri = ContentUris.withAppendedId(queryUri, id)
                    imageList.add(GalleryItem(uri = contentUri, dateAdded = dateAdded, width = width, height = height))
                }
            }
            _timelineItems.value = createTimeline(imageList)
        }
    }

    private fun createTimeline(images: List<GalleryItem>): List<TimelineItem> {
        val timeline = mutableListOf<TimelineItem>()
        val groupedByDate = images.groupBy { getFormattedDate(it.dateAdded) }

        for ((date, items) in groupedByDate) {
            timeline.add(TimelineItem.HeaderItem(date))
            timeline.addAll(items.map { TimelineItem.ImageItem(it) })
        }
        return timeline
    }

    private fun getFormattedDate(timestampSeconds: Long): String {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = calendar.get(Calendar.DAY_OF_YEAR)

        val itemCalendar = Calendar.getInstance().apply {
            timeInMillis = timestampSeconds * 1000
        }

        return when (itemCalendar.get(Calendar.DAY_OF_YEAR)) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(itemCalendar.time)
        }
    }
}
