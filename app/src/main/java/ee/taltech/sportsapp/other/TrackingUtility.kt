package ee.taltech.sportsapp.other

import android.content.Context
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.model.LatLng
import org.json.JSONObject
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object TrackingUtility {

    private var locationCodes: HashMap<String, String> = hashMapOf("LOC" to "00000000-0000-0000-0000-000000000001", "WP" to "00000000-0000-0000-0000-000000000002", "CP" to "00000000-0000-0000-0000-000000000003")
    private var loggingTag = "TRACKING"
    private var lastPathPoint = Location("")

    private var wasOffline = false
    private var unsentLocations = ArrayList<Location>()
    private var unsentLocationsTimeStamps = HashMap<Location, String>()
    private var unsentCPs = ArrayList<Location>()
    private var unsentCPTimestamps = HashMap<Location, String>()
    private var unsentWPs = ArrayList<Location>()
    private var unsentWPTimestamps = HashMap<Location, String>()

    fun setLastPathPoint(location: Location) {
        lastPathPoint = location
    }

    fun getLastPathPoint(): Location {
        return lastPathPoint
    }

    fun trySendingLocationData(context: Context, location: Location, locationType: String) {
        if(isOnline(context) && !wasOffline) {
            Log.d(loggingTag, "Online")
            sendLocationData(location, locationType, Volley.newRequestQueue(context), LocalDateTime.now().toString())
        } else if(isOnline(context) && wasOffline) {
            Log.d(loggingTag, "Online, was offline")
            if (locationType == "LOC") {
                if(unsentLocations.isNotEmpty()) {
                    for (locationElement in unsentLocations) {
                        Log.d(loggingTag, "Sending unsentLocations")
                        if (unsentLocationsTimeStamps.containsKey(locationElement)) {
                            sendLocationData(locationElement, "LOC", Volley.newRequestQueue(context), unsentLocationsTimeStamps[locationElement]!!)
                        }
                    }
                }
            } else if (locationType == "CP") {
                if(unsentCPs.isNotEmpty()) {
                    for (locationElement in unsentCPs) {
                        if (unsentCPTimestamps.containsKey(locationElement)) {
                            sendLocationData(locationElement, "CP", Volley.newRequestQueue(context), unsentCPTimestamps[locationElement]!!)
                        }
                    }
                }
            } else if(locationType == "WP") {
                if(unsentWPs.isNotEmpty()) {
                    for (locationElement in unsentWPs) {
                        if (unsentWPTimestamps.containsKey(locationElement)) {
                            sendLocationData(locationElement, "WP", Volley.newRequestQueue(context), unsentWPTimestamps[locationElement]!!)
                        }
                    }
                }
            } else {
                Log.d(loggingTag, "Wrong locationtype")
            }
            wasOffline = false
        } else {
            Log.d(loggingTag, "Not online....")
            wasOffline = true
            when (locationType) {
                "LOC" -> {
                    unsentLocations.add(location)
                    unsentLocationsTimeStamps[location] = LocalDateTime.now().toString()
                }
                "CP" -> {
                    unsentCPs.add(location)
                    unsentCPTimestamps[location] = LocalDateTime.now().toString()
                }
                "WP" -> {
                    unsentWPs.add(location)
                    unsentWPTimestamps[location] = LocalDateTime.now().toString()
                }
                else -> {
                    Log.d(loggingTag, "Wrong locationtype")
                }
            }
        }
    }

    private fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                    return true
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                    return true
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                    return true
                }
            }
        }
        return false
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
        var newText = inputMeters.toString() + "m"
        if (inputMeters > 1000) {
            var metersasDouble = inputMeters.toDouble()
            metersasDouble /= 1000.0
            val formattedValue = "%.2f".format(metersasDouble)
            newText = formattedValue + "km"
        }
        return newText
    }

    fun sendLocationData(location: Location, locationType: String, queue: RequestQueue, recordedAt: String) {
        val locationCode = locationCodes[locationType]
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

        val request = object: JsonObjectRequest(
            Method.POST,url,jsonObject,
            { response ->
                try {
                }catch (e:Exception){
                    Log.d(loggingTag, e.toString())
                }
            }, {
                Log.d(loggingTag, "Error in request hue")
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