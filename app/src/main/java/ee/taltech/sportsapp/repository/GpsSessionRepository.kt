package ee.taltech.sportsapp.repository

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ee.taltech.sportsapp.models.GpsSession
import ee.taltech.sportsapp.models.LatLngWithTime
import java.lang.reflect.Type

class GpsSessionRepository(val context: Context) {
    private lateinit var dbHelper: DbHelper
    private lateinit var db: SQLiteDatabase
    private var gson = Gson()
    val typeToken: Type = object : TypeToken<MutableList<MutableList<LatLngWithTime>>>() {}.type
    val typeTokenCP: Type = object : TypeToken<List<LatLng>>() {}.type

    fun open(): GpsSessionRepository {
        Log.d("RobertMapsActivity", "REPOSITORY")
        dbHelper = DbHelper(context)
        db = dbHelper.writableDatabase
        return this
    }

    fun close(){
        dbHelper.close()
    }

    fun add(gpsSession: GpsSession){
        var contentValues = ContentValues()
        contentValues.put(DbHelper.SESSION_DESCRIPTION, gpsSession.description)
        contentValues.put(DbHelper.SESSION_DISTANCE, gpsSession.distance)
        contentValues.put(DbHelper.SESSION_DURATION, gpsSession.duration)
        contentValues.put(DbHelper.SESSION_RECORDED_AT, gpsSession.recordedAt)
        contentValues.put(DbHelper.SESSION_SPEED, gpsSession.speed)
        contentValues.put(DbHelper.SESSION_CLIMB, gpsSession.climb)
        contentValues.put(DbHelper.SESSION_DESCENT, gpsSession.descent)
        contentValues.put(DbHelper.SESSION_GPSSESSIONID, gpsSession.gpsSessionId)
        contentValues.put(DbHelper.SESSION_APPUSERID, gpsSession.appUserId)
        contentValues.put(DbHelper.SESSION_NAME, gpsSession.name)
        contentValues.putNull(DbHelper.SESSION_ID)
        contentValues.put(DbHelper.SESSION_LATLNG, gson.toJson(gpsSession.latLng))
        contentValues.put(DbHelper.SESSION_CHECKPOINTS, gson.toJson(gpsSession.checkpoints))

        db.insert(DbHelper.TABLE_SESSIONS, null, contentValues)
    }

    fun getAll(): List<GpsSession> {
        val persons = ArrayList<GpsSession>()
        val columns = arrayOf(DbHelper.SESSION_DESCRIPTION, DbHelper.SESSION_DISTANCE, DbHelper.SESSION_DURATION,
            DbHelper.SESSION_RECORDED_AT, DbHelper.SESSION_SPEED, DbHelper.SESSION_CLIMB, DbHelper.SESSION_DESCENT,
            DbHelper.SESSION_GPSSESSIONID, DbHelper.SESSION_APPUSERID, DbHelper.SESSION_NAME, DbHelper.SESSION_ID,
            DbHelper.SESSION_LATLNG, DbHelper.SESSION_CHECKPOINTS)

        val cursor = db.query(DbHelper.TABLE_SESSIONS, columns, null, null, null, null, DbHelper.SESSION_ID + ", " + DbHelper.SESSION_NAME)

        while(cursor.moveToNext()){
            persons.add(
                GpsSession(
                    cursor.getString(cursor.getColumnIndex(DbHelper.SESSION_NAME)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.SESSION_DESCRIPTION)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.SESSION_RECORDED_AT)),
                    cursor.getInt(cursor.getColumnIndex(DbHelper.SESSION_ID)),
                    cursor.getDouble(cursor.getColumnIndex(DbHelper.SESSION_DURATION)),
                    cursor.getDouble(cursor.getColumnIndex(DbHelper.SESSION_SPEED)),
                    cursor.getDouble(cursor.getColumnIndex(DbHelper.SESSION_DISTANCE)),
                    cursor.getDouble(cursor.getColumnIndex(DbHelper.SESSION_CLIMB)),
                    cursor.getDouble(cursor.getColumnIndex(DbHelper.SESSION_DESCENT)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.SESSION_APPUSERID)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.SESSION_GPSSESSIONID)),
                    gson.fromJson(cursor.getString(cursor.getColumnIndex(DbHelper.SESSION_LATLNG)), typeToken),
                    gson.fromJson(cursor.getString(cursor.getColumnIndex(DbHelper.SESSION_CHECKPOINTS)), typeTokenCP)
            ))
        }

        cursor.close()

        return persons
    }

}
