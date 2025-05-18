package io.github.yearsyan.yaad.downloader // Or your appropriate package

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import io.github.yearsyan.yaad.R

object DownloadServiceConstants {
    const val NOTIFICATION_CHANNEL_ID = "DownloadChannel"
    const val NOTIFICATION_CHANNEL_NAME = "Download Service"
    const val NOTIFICATION_ID = 101
    const val ACTION_START_FOREGROUND_SERVICE =
        "ACTION_START_FOREGROUND_SERVICE"
    const val ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE"
}

class DownloadForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        when (intent?.action) {
            DownloadServiceConstants.ACTION_START_FOREGROUND_SERVICE -> {
                startForeground(
                    DownloadServiceConstants.NOTIFICATION_ID,
                    createNotification()
                )
            }
            DownloadServiceConstants.ACTION_STOP_FOREGROUND_SERVICE -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun createNotification(): Notification {
        val builder =
            NotificationCompat.Builder(
                    this,
                    DownloadServiceConstants.NOTIFICATION_CHANNEL_ID
                )
                .setContentTitle("Download Service Active")
                .setContentText("Downloads are in progress...")
                .setSmallIcon(R.drawable.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel =
                NotificationChannel(
                    DownloadServiceConstants.NOTIFICATION_CHANNEL_ID,
                    DownloadServiceConstants.NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
