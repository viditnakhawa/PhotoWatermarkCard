package com.viditnakhawa.photowatermarkcard.observers

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.provider.MediaStore
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.viditnakhawa.photowatermarkcard.workers.AutoFrameWorker

class NewImageObserver(
    private val context: Context,
    private val handler: Handler
) : ContentObserver(handler) {

    private val debounceJobs = mutableMapOf<Uri, Runnable>()

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)

        if (uri != null) {
            debounceJobs[uri]?.let { handler.removeCallbacks(it) }

            val job = Runnable {
                if (isNewCameraImage(context, uri)) {
                    println("NewImageObserver: Debounced change detected for new camera image at $uri")
                    startFramingWorker(uri)
                }
                debounceJobs.remove(uri)
            }

            handler.postDelayed(job, 1000L)
            debounceJobs[uri] = job
        }
    }

    private fun isNewCameraImage(context: Context, uri: Uri): Boolean {
        val projection = arrayOf(
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED
        )
        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                    val dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED))
                    val isRecent = (System.currentTimeMillis() / 1000) - dateAdded < 15
                    val isFromCamera = path != null && path.contains("/DCIM/", true)
                    val isScreenshot = path != null && path.contains("/Screenshots/", true)
                    val isAlreadyFramed = path != null && path.contains("/AutoFramed/", true)
                    return isRecent && isFromCamera && !isScreenshot && !isAlreadyFramed
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun startFramingWorker(uri: Uri) {
        val workManager = WorkManager.getInstance(context)
        val workRequest = OneTimeWorkRequestBuilder<AutoFrameWorker>()
            .setInputData(workDataOf(AutoFrameWorker.KEY_IMAGE_URI to uri.toString()))
            .build()

        workManager.enqueue(workRequest)
    }
}
