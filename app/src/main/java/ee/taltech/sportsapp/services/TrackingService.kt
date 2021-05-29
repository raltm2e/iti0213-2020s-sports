package ee.taltech.sportsapp.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import ee.taltech.sportsapp.MapsActivity
import ee.taltech.sportsapp.R
import ee.taltech.sportsapp.other.Constants.ACTION_PAUSE_SERVICE
import ee.taltech.sportsapp.other.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import ee.taltech.sportsapp.other.Constants.ACTION_START_OR_RESUME_SERVICE
import ee.taltech.sportsapp.other.Constants.ACTION_STOP_SERVICE
import ee.taltech.sportsapp.other.Constants.NOTIFICATION_CHANNEL_ID
import ee.taltech.sportsapp.other.Constants.NOTIFICATION_CHANNEL_NAME
import ee.taltech.sportsapp.other.Constants.NOTIFICATION_ID

class TrackingService : LifecycleService() {
    var loggingTag = "TRACKING"
    var isFirstRun = true

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let{
            when(it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    if(isFirstRun) {
                        Log.d(loggingTag, "Started service")
                        startForegroundService()
                        isFirstRun = false
                    } else {
                        Log.d(loggingTag, "Resuming service...")
                    }
                }
                ACTION_PAUSE_SERVICE -> {
                    Log.d(loggingTag, "Paused service")
                }
                ACTION_STOP_SERVICE -> {
                    Log.d(loggingTag, "Stopped service")
                }
                else -> {
                    Log.d(loggingTag, "Nothing")
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForegroundService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)
        Log.d(loggingTag, "Starting foreground service")

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Sportsapp")
            .setContentText("00:00:00")
            .setContentIntent(getMainActivityPendingIntent())

        Log.d(loggingTag, "Made notificationbuilder")
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
        Log.d(loggingTag, "Created notification")
    }

    private fun getMainActivityPendingIntent() = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MapsActivity::class.java).also {
            it.action = ACTION_SHOW_TRACKING_FRAGMENT
        },
        FLAG_UPDATE_CURRENT
    )

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

}