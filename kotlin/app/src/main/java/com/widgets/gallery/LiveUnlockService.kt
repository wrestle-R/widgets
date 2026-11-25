package com.widgets.gallery

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

// Keeping the process foreground lets WidgetsApp receive unlocks on Android 14+.
class LiveUnlockService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Live unlock count", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 0, Intent(this, LiveUnlockService::class.java).setAction(STOP), PendingIntent.FLAG_IMMUTABLE)
        startForeground(NOTIFICATION_ID, Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_lock_keyhole)
            .setContentTitle("Live unlock count is on")
            .setContentText("Updates your home screen widget when you unlock.")
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, "Stop", stop).build())
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setOngoing(true)
            .build())

        if (!Readings.hasUsageAccess(this) || UnlockRefresh.ids(this).isEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }
        running = true
        UnlockRefresh.enqueue(this)
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        super.onDestroy()
    }

    companion object {
        private const val STOP = "com.widgets.gallery.STOP_LIVE_UNLOCKS"
        private const val CHANNEL = "live_unlocks"
        private const val NOTIFICATION_ID = 86

        @Volatile var running = false
            private set

        fun start(context: Context): Boolean = try {
            context.startForegroundService(Intent(context, LiveUnlockService::class.java))
            running = true
            true
        } catch (_: IllegalStateException) {
            false
        } catch (_: SecurityException) {
            false
        }

        fun stop(context: Context) {
            running = false
            context.stopService(Intent(context, LiveUnlockService::class.java))
        }
    }
}
