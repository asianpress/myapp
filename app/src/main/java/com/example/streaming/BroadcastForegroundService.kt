package com.example.streaming

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class BroadcastForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Asian Press Live"
        val body = intent?.getStringExtra(EXTRA_BODY) ?: "Broadcasting live with studio overlays"
        val notification = buildNotification(title, body)
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Live Broadcast Studio",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active live stream and studio recording status"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, body: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "asian_press_live_channel"
        private const val NOTIFICATION_ID = 4201
        const val ACTION_START = "com.example.streaming.START"
        const val ACTION_STOP = "com.example.streaming.STOP"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"

        fun start(context: Context, title: String, body: String) {
            val intent = Intent(context, BroadcastForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_BODY, body)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BroadcastForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            runCatching { context.startService(intent) }
        }
    }
}
