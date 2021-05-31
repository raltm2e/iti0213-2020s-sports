package ee.taltech.sportsapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.gson.Gson
import ee.taltech.sportsapp.databinding.ActivityMapsBinding
import ee.taltech.sportsapp.models.GpsSession
import ee.taltech.sportsapp.other.Constants
import ee.taltech.sportsapp.other.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import ee.taltech.sportsapp.other.Constants.ACTION_START_OR_RESUME_SERVICE
import ee.taltech.sportsapp.other.Constants.ACTION_STOP_SERVICE
import ee.taltech.sportsapp.other.Constants.CYCLING_FAST
import ee.taltech.sportsapp.other.Constants.CYCLING_SLOW
import ee.taltech.sportsapp.other.Constants.MAP_SHOW
import ee.taltech.sportsapp.other.Constants.MAP_ZOOM
import ee.taltech.sportsapp.other.Constants.POLYLINE_COLOR
import ee.taltech.sportsapp.other.Constants.POLYLINE_COLOR_FAST
import ee.taltech.sportsapp.other.Constants.POLYLINE_COLOR_SLOW
import ee.taltech.sportsapp.other.Constants.POLYLINE_WIDTH
import ee.taltech.sportsapp.other.Constants.RUNNING_FAST
import ee.taltech.sportsapp.other.Constants.RUNNING_SLOW
import ee.taltech.sportsapp.other.Constants.SESSION_DISPLAY
import ee.taltech.sportsapp.other.Constants.UPDATE_MAP
import ee.taltech.sportsapp.other.Constants.WALKING_FAST
import ee.taltech.sportsapp.other.Constants.WALKING_SLOW
import ee.taltech.sportsapp.other.TrackingUtility
import ee.taltech.sportsapp.other.TrackingUtility.getSpeedBetweenLocations
import ee.taltech.sportsapp.other.TrackingUtility.trySendingLocationData
import ee.taltech.sportsapp.repository.GpsSessionRepository
import ee.taltech.sportsapp.services.Polyline
import ee.taltech.sportsapp.services.TrackingService
import kotlinx.android.synthetic.main.activity_maps.*
import kotlin.math.pow
import kotlin.math.roundToInt

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private var logtag = "RobertMapsActivity"
    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var repository: GpsSessionRepository
    private var showPreviousSession = false
    private var gson = Gson()
    private lateinit var sessionToDraw: GpsSession

    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()
    private var checkPoints = ArrayList<LatLng>()
    private var metersOnNewCP: Double = 0.0
    private lateinit var wayPoint: Marker
    private var wpExists = false
    private var wpPressed = false

    private var curTimeMillis = 0L

    fun drawMapFromSession() {
        val lastList = sessionToDraw.latLng.last()
        for (i in 0..lastList.size - 2) {
            val firstPoint = lastList[i]
            val secondPoint = lastList[i + 1]
            map.addPolyline(PolylineOptions()
                .add(firstPoint.latlng)
                .add(secondPoint.latlng)
                .color(Color.RED)
                .width(POLYLINE_WIDTH)
                )
        }
    }

    private fun resetValues() {
        pathPoints = mutableListOf()
        checkPoints = ArrayList()
        metersOnNewCP = 0.0
        if (wpExists) {
            wayPoint.isVisible = false
        }
        wpExists = false
        wpPressed = false
        curTimeMillis = 0L
    }

    var previousSessionBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            showPreviousSession = intent.getBooleanExtra(MAP_SHOW, false)
            sessionToDraw = gson.fromJson(intent.getStringExtra(SESSION_DISPLAY), GpsSession::class.java)
            Log.d(logtag, "Should Show Previous Session: ${showPreviousSession.toString()}")
            drawMapFromSession()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(logtag, "OnCreate")
        super.onCreate(savedInstanceState)

        LocalBroadcastManager.getInstance(this).registerReceiver(previousSessionBroadcastReceiver, IntentFilter(
            UPDATE_MAP))

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        navigateToTrackingFragmentIfNeeded(intent)

        // Moving between activities
        val compassButton = findViewById<Button>(R.id.buttonCompass)
        compassButton.setOnClickListener {
            val intent = Intent(this, CompassActivity::class.java)
            startActivity(intent)
        }
        val sessionsButton = findViewById<Button>(R.id.buttonSessions)
        sessionsButton.setOnClickListener {
            val intent = Intent(this, SessionsActivity::class.java)
            startActivity(intent)
        }
        val optionsButton = findViewById<Button>(R.id.buttonOptions)
        optionsButton.setOnClickListener {
            val intent = Intent(this, OptionsActivity::class.java)
            startActivity(intent)
        }
        val toggleRunButton = findViewById<Button>(R.id.buttonToggleRun)
        toggleRunButton.setOnClickListener {
            toggleRun()
        }
        val toggleCPButton = findViewById<Button>(R.id.buttonAddCP)
        toggleCPButton.setOnClickListener {
            addCheckpoint()
        }
        val toggleWPButton = findViewById<Button>(R.id.buttonAddWP)
        toggleWPButton.setOnClickListener {
            wpPressed = true
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(logtag, "onResume")
        addAllPolylines()
    }

    override fun onMapLongClick(latlng: LatLng) {
        if (wpPressed) {
            if (wpExists) {
                wayPoint.remove()
            }
            val location = LatLng(latlng.latitude, latlng.longitude)
            val markerOptions = MarkerOptions()
                .position(location)
                .icon(BitmapDescriptorFactory
                    .defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
            wayPoint = map.addMarker(markerOptions)
            wpExists = true

            val locationNew = Location("")
            locationNew.latitude = latlng.latitude
            locationNew.longitude = latlng.longitude
            trySendingLocationData(this, locationNew, "WP")
        }
        wpPressed = false
    }

    private fun addCheckpoint() {
        Log.d(logtag, "AddCheckpoint")
        if (pathPoints.isNotEmpty()) {
            val lastLatLng = pathPoints.last().last()
            checkPoints.add(lastLatLng.latlng)
            map.addMarker(MarkerOptions()
                .position(lastLatLng.latlng)
                .icon(BitmapDescriptorFactory
                    .defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))
            metersOnNewCP = TrackingService.travelledMeters

            trySendingLocationData(this, TrackingUtility.getLastPathPoint(), "CP")
        }
    }

    private fun updateCPDistance() {
        if (checkPoints.isNotEmpty()) {
            val distanceFromCP = (TrackingService.travelledMeters - metersOnNewCP).roundToInt()
            val textviewValue = TrackingUtility.metersToKilometers(distanceFromCP)
            textViewDistanceFromCP.text = textviewValue
            textViewDistanceFromWP.text = textviewValue
            if (pathPoints.last().isNotEmpty()) {
                updateCPDirect()
                updateWPDirect()
            }
        }
    }

    private fun updateCPDirect() {
        val lastLatLng = pathPoints.last().last()
        val lastCPLatLng = checkPoints.last()
        val CPDirect = TrackingUtility.getDistanceBetweenLocations(lastLatLng.latlng, lastCPLatLng).toInt()
        val textviewValue = TrackingUtility.metersToKilometers(CPDirect)
        textViewDirectFromCP.text = textviewValue
    }

    private fun updateWPDirect() {
        if (wpExists) {
            val lastLatLng = pathPoints.last().last()
            val lastWPLatLng = wayPoint.position
            val WPDirect = TrackingUtility.getDistanceBetweenLocations(lastLatLng.latlng, lastWPLatLng).toInt()
            val textviewValue = TrackingUtility.metersToKilometers(WPDirect)
            textViewDirectFromWP.text = textviewValue
        }
    }

    private fun resetMap() {
        Log.d(logtag, "Resetting values")
        resetValues()
        Log.d(logtag, "Resetting map")
        map.clear()
        Log.d(logtag, "All has been reset")
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if(intent?.action == "Reset") {
            resetMap()
        }
        navigateToTrackingFragmentIfNeeded(intent)
    }

    private fun subscribeToObservers() {
        TrackingService.isTracking.observe(this, Observer {
            updateTracking(it)
        })

        TrackingService.pathPoints.observe(this, Observer {
            pathPoints = it
            addLatestPolyline()
            updateDistanceTravelled()
            moveCameraToUser()
        })

        TrackingService.timeRunInMillis.observe(this, Observer {
            curTimeMillis = it
            val formattedTime = TrackingUtility.getFormattedStopWatchTime(curTimeMillis, false)
            textViewSessionDuration.text = formattedTime
        })
    }

    private fun updateDistanceTravelled() {
        val meters = TrackingService.travelledMeters.roundToInt()
        textViewDistanceCovered.text = TrackingUtility.metersToKilometers(meters)
        updatePace()
        updateCPDistance()
    }

    private fun updatePace() {
        if (TrackingService.avgPace < 99.9 && TrackingService.avgPace > 0.0) {
            val formattedValue = "%.2f".format(TrackingService.avgPace) + "min/km"
            textViewAverageSpeed.text = formattedValue
            updateSpeed()
        }
    }

    private fun updateSpeed() {
        val formattedValue = "%.2f".format(TrackingService.avgPace).toFloat()
        val speed = (formattedValue / 60).pow(-1) // min/km to km/hour
        val againFormattedValue = "%.2f".format(speed) + "km/h"
        textViewAverageSpeed1.text = againFormattedValue
        textViewAverageSpeed2.text = againFormattedValue
    }

    private fun toggleRun() {
        if(isTracking) {
            Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show()
            sendCommandToService(ACTION_STOP_SERVICE)
        } else {
            Toast.makeText(this, "Started training", Toast.LENGTH_SHORT).show()
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
    }

    private fun updateTracking(isTracking: Boolean) {
        this.isTracking = isTracking
        if(!isTracking) {
            buttonToggleRun.setBackgroundResource(android.R.drawable.ic_media_play)
        } else {
            buttonToggleRun.setBackgroundResource(android.R.drawable.ic_media_pause)
        }
    }

    private fun moveCameraToUser() {
        Log.d(logtag, "moveCameraToUser")
        if(pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()) {
            Log.d(logtag, "animateCamera")
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    pathPoints.last().last().latlng,
                    MAP_ZOOM
                )
            )
        }
    }

    private fun addAllPolylines() {
        Log.d(logtag, "addAllPolyLines")
        for(polylinewithtime in pathPoints) {
            var polylineColor = POLYLINE_COLOR
            if (polylinewithtime.size > 1) {
                val lastLatLng = polylinewithtime.last()
                val preLastLatLng = polylinewithtime[polylinewithtime.size - 2]
                polylineColor = getPolylineColor(getSpeedBetweenLocations(preLastLatLng, lastLatLng))
            }

            val polyline = ArrayList<LatLng>()
            for (element in polylinewithtime) {
                polyline.add(element.latlng)
            }

            val polylineOptions = PolylineOptions()
                .color(polylineColor)
                .width(POLYLINE_WIDTH)
                .addAll(polyline)
            map.addPolyline(polylineOptions)
        }
    }

    private fun addLatestPolyline() {
        Log.d(logtag, "addLatestPolyLine")
        if(pathPoints.isNotEmpty() && pathPoints.last().size > 1) {
            val preLastLatLng = pathPoints.last()[pathPoints.last().size - 2]
            val lastLatLng = pathPoints.last().last()

            val polylineColor = getPolylineColor(getSpeedBetweenLocations(preLastLatLng, lastLatLng))

            val polylineOptions = PolylineOptions()
                .color(polylineColor)
                .width(POLYLINE_WIDTH)
                .add(preLastLatLng.latlng)
                .add(lastLatLng.latlng)
            map.addPolyline(polylineOptions)
        }
    }

    private fun getPolylineColor(polylineSpeed: Float): Int {
        Log.d(logtag, "Polylinespeed: $polylineSpeed")
        var slowSpeed = RUNNING_SLOW
        var fastSpeed = RUNNING_FAST

        if(Constants.EXERCISE_TYPE == "Walking") {
            slowSpeed = WALKING_SLOW
            fastSpeed = WALKING_FAST
        } else if(Constants.EXERCISE_TYPE == "Cycling") {
            slowSpeed = CYCLING_SLOW
            fastSpeed = CYCLING_FAST
        }
        var polylineColor = POLYLINE_COLOR
        if(polylineSpeed < slowSpeed) {
            Log.d(logtag, "slowline")
            polylineColor = POLYLINE_COLOR_SLOW
        } else if(polylineSpeed > fastSpeed) {
            Log.d(logtag, "fastline")
            polylineColor = POLYLINE_COLOR_FAST
        }
        return polylineColor
    }

    private fun sendCommandToService(action: String) =
        Intent(this, TrackingService::class.java).also {
            it.action = action
            this.startService(it)
        }

    private fun navigateToTrackingFragmentIfNeeded(intent: Intent?) {
        if(intent?.action == ACTION_SHOW_TRACKING_FRAGMENT) {
            startActivity(intent)
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        // Add a marker in Sydney and move the camera
        val estonia = LatLng(59.0, 26.0)
        map.moveCamera(CameraUpdateFactory.newLatLng(estonia))
        map.setOnMapLongClickListener(this)
        subscribeToObservers()
    }
}