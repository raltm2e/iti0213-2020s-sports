package ee.taltech.sportsapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ee.taltech.sportsapp.other.RecyclerViewAdapterCustom
import ee.taltech.sportsapp.repository.GpsSessionRepository
import kotlinx.android.synthetic.main.activity_sessions.*

class SessionsActivity : AppCompatActivity() {

    private lateinit var adapterCustom: RecyclerView.Adapter<*>
    private lateinit var repository: GpsSessionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sessions)

        repository = GpsSessionRepository(this).open()
        gpsSessions.layoutManager = LinearLayoutManager(this)
        adapterCustom = RecyclerViewAdapterCustom(this, repository)
        gpsSessions.adapter = adapterCustom

    }
}