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
    handler: Handler
) : ContentObserver(handler) {

    private var lastProcessedUri: Uri? = null

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)

        if (uri != null && uri != lastProcessedUri) {
            // To prevent processing the same image multiple times in quick succession
            lastProcessedUri = uri

            // Check if the image is in the primary pictures directory and not in our own output folder
            if (isNewCameraImage(context, uri)) {
                println("NewImageObserver: Detected new camera image at $uri")
                startFramingWorker(uri)
            }
        }
    }

    private fun isNewCameraImage(context: Context, uri: Uri): Boolean {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))

                return (path.contains("/DCIM/", true) || path.contains("/Pictures/", true)) && !path.contains("/AutoFramed/", true)
            }
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
