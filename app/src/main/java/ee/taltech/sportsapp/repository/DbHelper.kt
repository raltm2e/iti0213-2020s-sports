package ee.taltech.sportsapp.repository

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DbHelper(context: Context): SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val DATABASE_NAME = "app.db"
        const val DATABASE_VERSION = 1

        const val TABLE_SESSIONS = "SESSIONS"
        const val SESSION_ID = "_id"
        const val SESSION_NAME = "name"
        const val SESSION_DESCRIPTION = "description"
        const val SESSION_RECORDED_AT = "recordedAt"
        const val SESSION_DURATION = "duration"
        const val SESSION_SPEED = "speed"
        const val SESSION_DISTANCE = "distance"
        const val SESSION_CLIMB = "climb"
        const val SESSION_DESCENT = "descent"
        const val SESSION_APPUSERID = "userid"
        const val SESSION_GPSSESSIONID = "gpsSessionId"

        const val SQL_CREATE_TABLES =
            "create table $TABLE_SESSIONS (" +
                    "$SESSION_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$SESSION_DESCRIPTION TEXT NOT NULL, " +
                    "$SESSION_NAME TEXT NOT NULL, " +
                    "$SESSION_RECORDED_AT TEXT NOT NULL, " +
                    "$SESSION_DURATION REAL NOT NULL, " +
                    "$SESSION_SPEED REAL NOT NULL, " +
                    "$SESSION_DISTANCE REAL NOT NULL, " +
                    "$SESSION_CLIMB REAl NOT NULL, " +
                    "$SESSION_DESCENT REAL NOT NULL, " +
                    "$SESSION_GPSSESSIONID TEXT NOT NULL, " +
                    "$SESSION_APPUSERID TEXT NOT NULL); "

        const val SQL_DELETE_TABLES =
            "DROP TABLE IF EXISTS $TABLE_SESSIONS;"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(SQL_CREATE_TABLES);
        Log.d("RobertMapsActivity", "DB CREATED")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL(SQL_DELETE_TABLES);
        onCreate(db);
    }
}
