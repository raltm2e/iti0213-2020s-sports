package ee.taltech.sportsapp.di

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import ee.taltech.sportsapp.R
import ee.taltech.sportsapp.other.Constants

object ServiceModule {

    fun provideBaseNotificationBuilder(app: Context, pendingIntent: PendingIntent): NotificationCompat.Builder {
        return NotificationCompat.Builder(app, Constants.NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Sportsapp")
            .setContentText("00:00:00")
            .setContentIntent(pendingIntent)
    }


}