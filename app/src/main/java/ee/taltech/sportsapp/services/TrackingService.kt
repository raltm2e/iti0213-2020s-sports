package ee.taltech.sportsapp.services

import android.content.Intent
import android.util.Log
import androidx.lifecycle.LifecycleService
import ee.taltech.sportsapp.other.Constants.ACTION_PAUSE_SERVICE
import ee.taltech.sportsapp.other.Constants.ACTION_START_OR_RESUME_SERVICE
import ee.taltech.sportsapp.other.Constants.ACTION_STOP_SERVICE

class TrackingService : LifecycleService() {
    var loggingTag = "TRACKING"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let{
            when(it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    Log.d(loggingTag, "Started or resumed service")
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

}