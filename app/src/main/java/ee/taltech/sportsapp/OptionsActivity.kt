package ee.taltech.sportsapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import ee.taltech.sportsapp.other.Constants
import ee.taltech.sportsapp.other.Variables
import kotlinx.android.synthetic.main.activity_options.*
import org.json.JSONObject

class OptionsActivity : AppCompatActivity() {

    var logtag = "OptionsActivity"

    lateinit var email: String
    lateinit var password: String
    lateinit var lastname: String
    lateinit var firstname: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_options)

        buttonLogIn.setOnClickListener {
            val emailText = findViewById<EditText>(R.id.editTextTextEmailAddress).text.toString()
            val passwordText = findViewById<EditText>(R.id.editTextTextPassword).text.toString()
            if (emailText.isNotEmpty() && passwordText.isNotEmpty()) {
                Log.d(logtag, "Email: $emailText, Password: $passwordText")
                email = emailText
                password = passwordText
                sendLoginRequest()
            } else {
                Toast.makeText(this, "Bad input", Toast.LENGTH_SHORT).show()
            }
        }

        buttonRegister.setOnClickListener {
            val emailText = findViewById<EditText>(R.id.editTextTextEmailAddressRegister).text.toString()
            val passwordText = findViewById<EditText>(R.id.editTextTextPasswordRegister).text.toString()
            val firstNameText = findViewById<EditText>(R.id.editTextTextFirstname).text.toString()
            val lastNameText = findViewById<EditText>(R.id.editTextTextLastname).text.toString()
            if (emailText.isNotEmpty() && passwordText.isNotEmpty() && firstNameText.isNotEmpty() && lastNameText.isNotEmpty()) {
                Log.d(logtag, "Email: $emailText, Password: $passwordText, Firstname: $firstNameText, Lastname: $lastNameText")
                email = emailText
                password = passwordText
                firstname = firstNameText
                lastname = lastNameText
                sendRegisterRequest()
            } else {
                Toast.makeText(this, "Bad input", Toast.LENGTH_SHORT).show()
            }
        }

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

    private fun sendLoginRequest() {
        val queue = Volley.newRequestQueue(this)
        val url = Constants.BASEURL + "account/login"
        Log.d(logtag, url)

        val params = HashMap<String,String>()
        params["email"] = email
        params["password"] = password
        val jsonObject = JSONObject(params as Map<*, *>)

        val request = object: JsonObjectRequest(
            Method.POST,url,jsonObject,
            { response ->
                try {
                    Toast.makeText(this, "Logged in", Toast.LENGTH_SHORT).show()
                    Variables.apiToken = response.getString("token")
                }catch (e:Exception){
                    Log.d(logtag, e.toString())
                }
            }, {
                Log.d(logtag, "Error in request")
                Toast.makeText(this, "Error in request", Toast.LENGTH_SHORT).show()
            })

        {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                return headers
            }
        }
        queue.add(request)
    }

    private fun sendRegisterRequest() {
        val queue = Volley.newRequestQueue(this)
        val url = Constants.BASEURL + "account/register"

        val params = HashMap<String,String>()
        params["email"] = email
        params["password"] = password
        params["firstName"] = firstname
        params["lastName"] = lastname
        val jsonObject = JSONObject(params as Map<*, *>)
        Log.d(logtag, jsonObject.toString(4))

        val request = object: JsonObjectRequest(Method.POST,url,jsonObject,
            { response ->
                try {
                    Toast.makeText(this, "Registered", Toast.LENGTH_SHORT).show()
                    Variables.apiToken = response.getString("token")
                }catch (e:Exception){
                    Log.d(logtag, e.toString())
                }
            }, {
                Log.d(logtag, "Error in request")
                Toast.makeText(this, "Error in request", Toast.LENGTH_SHORT).show()

            })
        {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                return headers
            }
        }
        queue.add(request)
    }
}