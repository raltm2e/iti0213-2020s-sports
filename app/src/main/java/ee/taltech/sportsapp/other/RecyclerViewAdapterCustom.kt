package ee.taltech.sportsapp.other

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import ee.taltech.sportsapp.R
import ee.taltech.sportsapp.models.GpsSession
import ee.taltech.sportsapp.other.Constants.MAP_SHOW
import ee.taltech.sportsapp.other.Constants.SESSION_DISPLAY
import ee.taltech.sportsapp.other.Constants.UPDATE_MAP
import ee.taltech.sportsapp.repository.GpsSessionRepository
import kotlinx.android.synthetic.main.rowview.view.*

class RecyclerViewAdapterCustom(val context: Context, val repository: GpsSessionRepository): RecyclerView.Adapter<RecyclerViewAdapterCustom.ViewHolder>() {

    lateinit var dataSet: List<GpsSession>

    fun refreshData() {
        dataSet = repository.getAll()
    }

    init {
        refreshData()
    }

    private val layoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val rowView = layoutInflater.inflate(R.layout.rowview, parent, false)
        return ViewHolder(rowView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val values = dataSet[position]
        holder.itemView.textViewName.text = values.name
        holder.itemView.textViewDate.text = values.recordedAt
        holder.itemView.textViewDescription.text = values.description
        val distanceFormatted = "%.2f".format(values.distance)
        holder.itemView.textViewDistance.text = distanceFormatted

        // Add onclick to buttons
        val openButton = holder.itemView.buttonOpen
        val gson = Gson()
        openButton.setOnClickListener {
            val intent = Intent(UPDATE_MAP)
            intent.putExtra(MAP_SHOW, true)
            intent.putExtra(SESSION_DISPLAY, gson.toJson(values))
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }

        holder.itemView.buttonExport.setOnClickListener {

        }
    }

    override fun getItemCount(): Int {
        return dataSet.count()
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)
}