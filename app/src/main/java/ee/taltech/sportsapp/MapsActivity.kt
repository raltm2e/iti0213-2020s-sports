package ee.taltech.sportsapp

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import ee.taltech.sportsapp.databinding.ActivityMapsBinding
import ee.taltech.sportsapp.other.Constants.ACTION_PAUSE_SERVICE
import ee.taltech.sportsapp.other.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import ee.taltech.sportsapp.other.Constants.ACTION_START_OR_RESUME_SERVICE
import ee.taltech.sportsapp.other.Constants.MAP_ZOOM
import ee.taltech.sportsapp.other.Constants.POLYLINE_COLOR
import ee.taltech.sportsapp.other.Constants.POLYLINE_WIDTH
import ee.taltech.sportsapp.other.TrackingUtility
import ee.taltech.sportsapp.services.Polyline
import ee.taltech.sportsapp.services.TrackingService
import kotlinx.android.synthetic.main.activity_maps.*
import kotlin.math.pow
import kotlin.math.roundToInt

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private var logtag = "MapsActivity"

    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()
    private var checkPoints = ArrayList<LatLng>()
    private var metersOnNewCP: Double = 0.0
    private lateinit var wayPoint: Marker
    private var wpExists = false
    private var wpPressed = false

    private var curTimeMillis = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        addAllPolylines()
        subscribeToObservers()

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
            TrackingUtility.sendLocationData(locationNew, "WP", Volley.newRequestQueue(this))
        }
        wpPressed = false
    }

    private fun addCheckpoint() {
        if (pathPoints.isNotEmpty()) {
            val lastLatLng = pathPoints.last().last()
            checkPoints.add(lastLatLng)
            map.addMarker(MarkerOptions()
                .position(lastLatLng)
                .icon(BitmapDescriptorFactory
                    .defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))
            metersOnNewCP = TrackingService.travelledMeters

            TrackingUtility.sendLocationData(TrackingUtility.getLastPathPoint(), "CP", Volley.newRequestQueue(this))
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
        val CPDirect = TrackingUtility.getDistanceBetweenLocations(lastLatLng, lastCPLatLng).toInt()
        val textviewValue = TrackingUtility.metersToKilometers(CPDirect)
        textViewDirectFromCP.text = textviewValue
    }

    private fun updateWPDirect() {
        if (wpExists) {
            val lastLatLng = pathPoints.last().last()
            val lastWPLatLng = wayPoint.position
            val WPDirect = TrackingUtility.getDistanceBetweenLocations(lastLatLng, lastWPLatLng).toInt()
            val textviewValue = TrackingUtility.metersToKilometers(WPDirect)
            textViewDirectFromWP.text = textviewValue
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
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
        Log.d(logtag, "Speed: $speed")
        val textfieldValue = speed.toString() + "km/h"
        textViewAverageSpeed1.text = textfieldValue
        textViewAverageSpeed2.text = textfieldValue
    }

    private fun toggleRun() {
        if(isTracking) {
            Toast.makeText(this, "Paused", Toast.LENGTH_SHORT).show()
            sendCommandToService(ACTION_PAUSE_SERVICE)
        } else {
            Toast.makeText(this, "Started", Toast.LENGTH_SHORT).show()
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
        if(pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()) {
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    pathPoints.last().last(),
                    MAP_ZOOM
                )
            )
        }
    }

    private fun addAllPolylines() {
        for(polyline in pathPoints) {
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .addAll(polyline)
            map.addPolyline(polylineOptions)
        }
    }

    private fun addLatestPolyline() {
        if(pathPoints.isNotEmpty() && pathPoints.last().size > 1) {
            val preLastLatLng = pathPoints.last()[pathPoints.last().size - 2]
            val lastLatLng = pathPoints.last().last()
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .add(preLastLatLng)
                .add(lastLatLng)
            map.addPolyline(polylineOptions)
        }
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
        //map.addMarker(MarkerOptions().position(estonia).title("Marker in Estonia"))
        map.moveCamera(CameraUpdateFactory.newLatLng(estonia))
        map.setOnMapLongClickListener(this)
    }
}