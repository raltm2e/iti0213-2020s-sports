package ee.taltech.sportsapp.other

import android.graphics.Color

object Constants {
    const val ACTION_START_OR_RESUME_SERVICE = "ACTION_START_OR_RESUME_SERVICE"
    const val ACTION_PAUSE_SERVICE = "ACTION_PAUSE_SERVICE"
    const val ACTION_SHOW_TRACKING_FRAGMENT = "ACTION_SHOW_TRACKING_FRAGMENT"

    const val TIMER_UPDATE_INTERVAL = 200L

    var LOCATION_UPDATE_INTERVAL = 5000L
    var FASTEST_LOCATION_INTERVAL = 3000L
    var DATA_SYNC_INTERVAL = 5000L
    var EXERCISE_TYPE = "Running"

    const val BASEURL = "https://sportmap.akaver.com/api/v1.0/"

    const val POLYLINE_COLOR = Color.RED
    const val POLYLINE_WIDTH = 8f
    const val MAP_ZOOM = 16f

    const val NOTIFICATION_CHANNEL_ID = "tracking_channel"
    const val NOTIFICATION_CHANNEL_NAME = "tracking"
    const val NOTIFICATION_ID = 1
}