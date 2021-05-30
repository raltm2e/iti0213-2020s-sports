package ee.taltech.sportsapp.other

import android.location.Location
import android.util.Log
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.gms.maps.model.LatLng
import org.json.JSONObject
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object TrackingUtility {

    private var locationCodes: HashMap<String, String> = hashMapOf("LOC" to "00000000-0000-0000-0000-000000000001", "WP" to "00000000-0000-0000-0000-000000000002", "CP" to "00000000-0000-0000-0000-000000000003")
    private var loggingTag = "TRACKING"
    private var lastPathPoint = Location("")

    fun setLastPathPoint(location: Location) {
        lastPathPoint = location
    }

    fun getLastPathPoint(): Location {
        return lastPathPoint
    }

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

    fun sendLocationData(location: Location, locationType: String, queue: RequestQueue) {
        val locationCode = locationCodes[locationType]
        val recordedAt = LocalDateTime.now().toString()

        val url = Constants.BASEURL + "GpsLocations"

        val params = HashMap<String,Any>()
        params["recordedAt"] = recordedAt
        params["latitude"] = location.latitude
        params["longitude"] = location.longitude
        params["accuracy"] = location.accuracy
        params["altitude"] = location.altitude
        params["verticalAccuracy"] = location.verticalAccuracyMeters
        params["gpsSessionId"] = Variables.sessionId
        params["gpsLocationTypeId"] = locationCode.toString()
        val jsonObject = JSONObject(params as Map<*, *>)
        Log.d(loggingTag, jsonObject.toString(4))

        val request = object: JsonObjectRequest(
            Method.POST,url,jsonObject,
            { response ->
                try {
                }catch (e:Exception){
                    Log.d(loggingTag, e.toString())
                }
            }, {
                Log.d(loggingTag, "Error in request")
            })

        {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                val token = Variables.apiToken
                headers["Content-Type"] = "application/json"
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }
        if (Variables.apiToken.isNotEmpty()) {
            queue.add(request)
        }
    }
}