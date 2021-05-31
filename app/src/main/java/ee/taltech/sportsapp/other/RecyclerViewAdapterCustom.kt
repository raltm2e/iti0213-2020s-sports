package ee.taltech.sportsapp.other

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ee.taltech.sportsapp.R
import ee.taltech.sportsapp.models.GpsSession
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
    }

    override fun getItemCount(): Int {
        return dataSet.count()
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)
}