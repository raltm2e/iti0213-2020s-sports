package ee.taltech.sportsapp.models

import ee.taltech.sportsapp.services.Polylines

data class GpsSession(
    var name: String,
    var description: String,
    var recordedAt: String,
    var id: Int,
    var duration: Double,
    var speed: Double,
    var distance: Double,
    var climb: Double,
    var descent: Double,
    var appUserId: String,
    var gpsSessionId: String,
    var latLng: Polylines
) {
}