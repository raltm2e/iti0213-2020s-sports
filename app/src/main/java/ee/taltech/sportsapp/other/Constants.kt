package ee.taltech.sportsapp.other

import android.graphics.Color

object Constants {
    const val ACTION_START_OR_RESUME_SERVICE = "ACTION_START_OR_RESUME_SERVICE"
    const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"

    const val MAP_SHOW = "SHOW_ON_MAP"
    const val UPDATE_MAP = "UPDATE_MAP"
    const val SESSION_DISPLAY = "SESSION_DISPLAY"

    const val TIMER_UPDATE_INTERVAL = 200L

    var LOCATION_UPDATE_INTERVAL = 15000L
    var FASTEST_LOCATION_INTERVAL = 13000L
    var DATA_SYNC_INTERVAL = "ON RECEIVE"
    var EXERCISE_TYPE = "Running"

    const val RUNNING_SLOW = 3.0
    const val RUNNING_FAST = 6.0
    const val WALKING_SLOW = 1.5
    const val WALKING_FAST = 4.5
    const val CYCLING_SLOW = 20.0
    const val CYCLING_FAST = 30.0

    const val BASEURL = "https://sportmap.akaver.com/api/v1.0/"

    const val POLYLINE_COLOR = Color.CYAN
    const val POLYLINE_COLOR_SLOW = Color.MAGENTA
    const val POLYLINE_COLOR_FAST = Color.GREEN
    const val POLYLINE_WIDTH = 8f
    const val MAP_ZOOM = 16f

    const val NOTIFICATION_CHANNEL_ID = "tracking_channel"
    const val NOTIFICATION_CHANNEL_NAME = "tracking"
    const val NOTIFICATION_ID = 1
}