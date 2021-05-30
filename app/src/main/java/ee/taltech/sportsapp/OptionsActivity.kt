package ee.taltech.sportsapp

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import ee.taltech.sportsapp.other.Constants
import kotlinx.android.synthetic.main.activity_options.*

class OptionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_options)

        spinnerGPSInterval.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                when {
                    parent?.getItemAtPosition(position).toString() == "5 sec" -> {
                        Constants.LOCATION_UPDATE_INTERVAL = 5000L
                    }
                    parent?.getItemAtPosition(position).toString() == "15 sec" -> {
                        Constants.LOCATION_UPDATE_INTERVAL = 15000L
                    }
                    else -> {
                        Constants.LOCATION_UPDATE_INTERVAL = 30000L
                    }
                }
                Constants.FASTEST_LOCATION_INTERVAL = Constants.LOCATION_UPDATE_INTERVAL - 2000L
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        spinnerSyncInterval.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                when {
                    parent?.getItemAtPosition(position).toString() == "5 sec" -> {
                        Constants.DATA_SYNC_INTERVAL = 5000L
                    }
                    parent?.getItemAtPosition(position).toString() == "15 sec" -> {
                        Constants.DATA_SYNC_INTERVAL = 15000L
                    }
                    else -> {
                        Constants.DATA_SYNC_INTERVAL = 30000L
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        spinnerExercise.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                Constants.EXERCISE_TYPE = parent?.getItemAtPosition(position).toString()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

    }
}