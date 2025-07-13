package com.viditnakhawa.photowatermarkcard.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import com.viditnakhawa.photowatermarkcard.R
import com.viditnakhawa.photowatermarkcard.observers.NewImageObserver
import android.app.PendingIntent
import androidx.core.net.toUri
import com.viditnakhawa.photowatermarkcard.MainActivity

class AutoFrameService : Service() {

    private lateinit var imageObserver: NewImageObserver
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        imageObserver = NewImageObserver(this, handler)
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            imageObserver
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        contentResolver.unregisterContentObserver(imageObserver)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotification(): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AutoFrame Service",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            "app://photowatermarkcard/automation".toUri(),
            this,
            MainActivity::class.java
        )
        val pendingIntent = PendingIntent.getActivity(
            this, 0, deepLinkIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AutoFrame Active")
            .setContentText("Watching for new photos to frame.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "AutoFrameServiceChannel"
        const val NOTIFICATION_ID = 1
    }
}
