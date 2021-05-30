package ee.taltech.sportsapp.other

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import java.util.concurrent.TimeUnit

object TrackingUtility {

    fun getFormattedStopWatchTime(ms: Long, includeMillis: Boolean = false): String {
        var milliseconds = ms
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        milliseconds -= TimeUnit.HOURS.toMillis(hours)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        milliseconds -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds)
        if(!includeMillis) {
            return "${if(hours < 10) "0" else ""}$hours:" +
                    "${if(minutes<10) "0" else ""}$minutes:" +
                    "${if(seconds<10) "0" else ""}$seconds"
        }
        milliseconds -= TimeUnit.SECONDS.toMillis(seconds)
        milliseconds /= 10
        return "${if(hours < 10) "0" else ""}$hours:" +
                "${if(minutes<10) "0" else ""}$minutes:" +
                "${if(seconds<10) "0" else ""}$seconds:" +
                "${if(milliseconds<10) "0" else ""}$milliseconds"
    }

    fun getDistanceBetweenLocations(latlng1: LatLng, latlng2: LatLng): Float {
        val location1 = Location("")
        location1.latitude = latlng1.latitude
        location1.longitude = latlng1.longitude

        val location2 = Location("")
        location2.latitude = latlng2.latitude
        location2.longitude = latlng2.longitude

        return location2.distanceTo(location1)
    }

    fun metersToKilometers(inputMeters: Int): String {
        var meters = inputMeters
        var newText = meters.toString() + "m"
        if (meters > 1000) {
            var metersasDouble = meters.toDouble()
            metersasDouble /= 1000.0
            val formattedValue = "%.2f".format(metersasDouble)
            newText = formattedValue.toString() + "km"
        }
        return newText
    }
}