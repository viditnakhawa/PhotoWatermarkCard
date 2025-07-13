package com.viditnakhawa.photowatermarkcard.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.viditnakhawa.photowatermarkcard.R

object NotificationUtils {

    private const val FRAMING_COMPLETE_CHANNEL_ID = "FramingCompleteChannel"
    private const val FRAMING_COMPLETE_NOTIFICATION_ID = 2 // Use a unique ID

    fun showFramingCompleteNotification(context: Context, framedImageUri: Uri) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            FRAMING_COMPLETE_CHANNEL_ID,
            "Framing Results",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Shows a notification when a photo has been successfully framed."
        }
        notificationManager.createNotificationChannel(channel)

        // Create an intent to open the image when the notification is tapped
        val viewIntent = Intent(Intent.ACTION_VIEW, framedImageUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val viewPendingIntent = PendingIntent.getActivity(
            context, 0, viewIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create a thumbnail for the notification
        val thumbnail = try {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, framedImageUri))
        } catch (e: Exception) {
            null
        }

        // Build the notification
        val notification = NotificationCompat.Builder(context, FRAMING_COMPLETE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with a dedicated icon if you have one
            .setContentTitle("Photo Framed")
            .setContentText("Tap to view your new framed photo.")
            .setLargeIcon(thumbnail)
            .setStyle(NotificationCompat.BigPictureStyle().bigPicture(thumbnail))
            .setContentIntent(viewPendingIntent)
            .setAutoCancel(true) // Notification disappears when tapped
            .build()

        notificationManager.notify(FRAMING_COMPLETE_NOTIFICATION_ID, notification)
    }
}