package com.viditnakhawa.photowatermarkcard.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.viditnakhawa.photowatermarkcard.utils.FrameUtils
import androidx.core.net.toUri

class AutoFrameWorker(
    private val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val imageUriString = inputData.getString(KEY_IMAGE_URI)
        if (imageUriString.isNullOrEmpty()) {
            return Result.failure()
        }

        val imageUri = imageUriString.toUri()

        return try {
            println("AutoFrameWorker: Received image URI: $imageUriString")

            val success = FrameUtils.processImage(context, imageUri)

            if (success) {
                Result.success()
            } else {
                Result.failure()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    companion object {
        const val KEY_IMAGE_URI = "key_image_uri"
    }
}
