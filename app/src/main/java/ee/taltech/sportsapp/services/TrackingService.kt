package ee.taltech.sportsapp.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import ee.taltech.sportsapp.MapsActivity
import ee.taltech.sportsapp.di.ServiceModule
import ee.taltech.sportsapp.models.GpsSession
import ee.taltech.sportsapp.models.LatLngWithTime
import ee.taltech.sportsapp.other.Constants
import ee.taltech.sportsapp.other.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import ee.taltech.sportsapp.other.Constants.ACTION_START_OR_RESUME_SERVICE
import ee.taltech.sportsapp.other.Constants.ACTION_STOP_SERVICE
import ee.taltech.sportsapp.other.Constants.FASTEST_LOCATION_INTERVAL
import ee.taltech.sportsapp.other.Constants.LOCATION_UPDATE_INTERVAL
import ee.taltech.sportsapp.other.Constants.NOTIFICATION_CHANNEL_ID
import ee.taltech.sportsapp.other.Constants.NOTIFICATION_CHANNEL_NAME
import ee.taltech.sportsapp.other.Constants.NOTIFICATION_ID
import ee.taltech.sportsapp.other.Constants.TIMER_UPDATE_INTERVAL
import ee.taltech.sportsapp.other.TrackingUtility
import ee.taltech.sportsapp.other.TrackingUtility.trySendingLocationData
import ee.taltech.sportsapp.other.Variables
import ee.taltech.sportsapp.repository.GpsSessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.Locale.getDefault
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

typealias Polyline = MutableList<LatLngWithTime>
typealias Polylines = MutableList<Polyline>

class TrackingService : LifecycleService() {
    private var loggingTag = "TRACKING"

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val timeRunInSeconds = MutableLiveData<Long>()

    lateinit var baseNotificationBuilder: NotificationCompat.Builder
    lateinit var curNotificationBuilder: NotificationCompat.Builder

    lateinit var repository: GpsSessionRepository

    companion object {
        val timeRunInMillis = MutableLiveData<Long>()
        val isTracking = MutableLiveData<Boolean>()
        var pathPoints = MutableLiveData<Polylines>()
        var travelledMeters = 0.0
        var avgPace = 0.0
    }

    private fun postInitialValues() {
        isTracking.postValue(false)
        pathPoints.postValue(mutableListOf())
        timeRunInSeconds.postValue(0L)
        timeRunInMillis.postValue(0L)
    }

    private fun resetValues() {
        isTracking.postValue(false)
        timeRunInSeconds.postValue(0L)
        timeRunInMillis.postValue(0L)
        lapTime = 0L
        timeRun = 0L
        lastSecondTimestamp = 0L
        pathPoints = MutableLiveData<Polylines>()
        travelledMeters = 0.0
        avgPace = 0.0
        val intent = Intent(this, MapsActivity::class.java).also {
            it.action = "Reset"
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        Log.d(loggingTag, "Starting activity for reset")
        startActivity(intent)
    }

    override fun onCreate() {
        super.onCreate()
        baseNotificationBuilder = ServiceModule.provideBaseNotificationBuilder(this, getMainActivityPendingIntent())
        curNotificationBuilder = baseNotificationBuilder
        postInitialValues()
        fusedLocationProviderClient = FusedLocationProviderClient(this)
        isTracking.observe(this, Observer {
            updateLocationTracking(it)
            updateNotificationTrackingState(it)
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let{
            when(it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    Log.d(loggingTag, "Started service")
                    startForegroundService()
                }
                ACTION_STOP_SERVICE -> {
                    Log.d(loggingTag, "Stopped service")
                    stopService()
                }
                else -> {
                    Log.d(loggingTag, "Nothing")
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private var isTimerEnabled = false
    private var lapTime = 0L
    private var timeRun = 0L
    private var timeStarted = 0L
    private var lastSecondTimestamp = 0L

    private fun startTimer() {
        addEmptyPolyline()
        isTracking.postValue(true)
        timeStarted = System.currentTimeMillis()
        isTimerEnabled = true
        CoroutineScope(Dispatchers.Main).launch {
            while(isTracking.value!!) {
                lapTime = System.currentTimeMillis() - timeStarted
                timeRunInMillis.postValue(timeRun + lapTime)
                if(timeRunInMillis.value!! >= lastSecondTimestamp + 1000L) {
                    timeRunInSeconds.postValue(timeRunInSeconds.value!! + 1)
                    lastSecondTimestamp += 1000L
                }
                delay(TIMER_UPDATE_INTERVAL)
            }
            timeRun += lapTime
        }
    }

    private fun stopService() {
        isTracking.postValue(false)
        isTimerEnabled = false
        endSession()
    }

    private fun endSession() {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", getDefault())
        repository.add(GpsSession("Nimi", "Randomdesc", formatter.format(Date()), 0,
            timeRunInSeconds.value!!.toDouble(), avgPace, travelledMeters, 0.0, 0.0, "", Variables.sessionId, pathPoints.value!!))
        resetValues()
        repository.close()
    }

    private fun updateNotificationTrackingState(isTracking: Boolean) {
        Log.d(loggingTag, "Update notification tracking")
        val pendingIntent = if(isTracking) {
            val pauseIntent = Intent(this, TrackingService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            PendingIntent.getService(this, 1, pauseIntent, FLAG_UPDATE_CURRENT)
        } else {
            val resumeIntent = Intent(this, TrackingService::class.java).apply {
                action = ACTION_START_OR_RESUME_SERVICE
            }
            PendingIntent.getService(this, 2, resumeIntent, FLAG_UPDATE_CURRENT)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        curNotificationBuilder.javaClass.getDeclaredField("mActions").apply {
            isAccessible = true
            set(curNotificationBuilder, ArrayList<NotificationCompat.Action>())
        }
        curNotificationBuilder = baseNotificationBuilder
        notificationManager.notify(NOTIFICATION_ID, curNotificationBuilder.build())
    }

    private fun updateLocationTracking(isTracking: Boolean) {
        if(isTracking) {
            val request = LocationRequest().apply {
                interval = LOCATION_UPDATE_INTERVAL
                fastestInterval = FASTEST_LOCATION_INTERVAL
                priority = PRIORITY_HIGH_ACCURACY
            }
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            fusedLocationProviderClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )
        }
        else {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult?) {
            super.onLocationResult(result)
            if(isTracking.value!!) {
                result?.locations?.let { locations ->
                    for(location in locations) {
                        addPathPoint(location)
                        Log.d(loggingTag, "NEW LOCATION: ${location.latitude}, ${location.longitude}")
                        TrackingUtility.setLastPathPoint(location)
                    }
                }
            }
        }
    }

    private fun increaseDistance(lastpos: LatLng) {
        val thisLocation = Location("")
        thisLocation.latitude = lastpos.latitude
        thisLocation.longitude = lastpos.longitude

        if (pathPoints.value?.last()?.size!! > 1) {
            val previousLatLng = pathPoints.value?.last()?.get(pathPoints.value?.last()!!.size - 2)
            val previousLocation = Location("")

            if (previousLatLng != null) {
                previousLocation.latitude = previousLatLng.latlng.latitude
                previousLocation.longitude = previousLatLng.latlng.longitude
            }

            val smallDistance = previousLocation.distanceTo(thisLocation)
            travelledMeters += smallDistance
            updateAvgPace()
            Log.d(loggingTag, travelledMeters.toString())
        }
    }

    private fun updateAvgPace() {
        // Minutes per 1 kilometer
        avgPace = (timeRunInSeconds.value!! / travelledMeters) * (1000 / 60) //Convert sec/m to min/km
        Log.d(loggingTag, "Updating pace: $avgPace min/km")
    }

    private fun addEmptyPolyline() = pathPoints.value?.apply {
        add(mutableListOf())
        pathPoints.postValue(this)
    } ?: pathPoints.postValue(mutableListOf(mutableListOf()))

    private fun addPathPoint(location: Location?) {
        location?.let {
            val pos = LatLng(location.latitude, location.longitude)
            val time = System.currentTimeMillis()
            val latLngWithTime = LatLngWithTime(pos, time)
            pathPoints.value?.apply {
                last().add(latLngWithTime)
                pathPoints.postValue(this)
            }
            increaseDistance(pos)

            trySendingLocationData(this, location, "LOC")
        }
    }

    private fun sendStartRequest() {
        val name = LocalDate.now().toString()
        val description = name + Constants.EXERCISE_TYPE
        val recordedAt = LocalDateTime.now().toString()
        val paceMin = 100
        val paceMax = 1000

        val queue = Volley.newRequestQueue(this)
        val url = Constants.BASEURL + "GpsSessions"

        val params = HashMap<String,Any>()
        params["name"] = name
        params["description"] = description
        params["recordedAt"] = recordedAt
        params["paceMin"] = paceMin
        params["paceMax"] = paceMax
        val jsonObject = JSONObject(params as Map<*, *>)

        Log.d(loggingTag, "Making start request")

        val request = object: JsonObjectRequest(
            Method.POST,url,jsonObject,
            { response ->
                try {
                    Log.d(loggingTag, "Making toast message")
                    Toast.makeText(this, "Started", Toast.LENGTH_SHORT).show()
                    Log.d(loggingTag, response.toString(4))
                    Variables.sessionId = response.getString("id")
                }catch (e:Exception){
                    Log.d(loggingTag, e.toString())
                }
            }, {
                Log.d(loggingTag, "Error in request")
                Toast.makeText(this, "Error in request", Toast.LENGTH_SHORT).show()
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

    private fun startForegroundService() {
        startTimer()
        isTracking.postValue(true)
        sendStartRequest()
        repository = GpsSessionRepository(this).open()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

        baseNotificationBuilder = ServiceModule.provideBaseNotificationBuilder(this, getMainActivityPendingIntent())

        startForeground(NOTIFICATION_ID, baseNotificationBuilder.build())

        timeRunInSeconds.observe(this, Observer {
            val notification = curNotificationBuilder
                .setContentText(TrackingUtility.getFormattedStopWatchTime(it * 1000, false))
            notificationManager.notify(NOTIFICATION_ID, notification.build())
        })
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