package ee.taltech.sportsapp.other

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import android.util.Xml
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
import org.xmlpull.v1.XmlSerializer
import java.io.File
import java.io.FileWriter
import java.io.StringWriter
import java.time.LocalDateTime


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
            // Generate XML https://github.com/dnowak/kotlin-xml-writer
            val writer = Xml.newSerializer()
            val gpx = writer.document {
                element("gpx") {
                    attribute("version", "1.1")
                    attribute("created", LocalDateTime.now().toString())
                    element("metadata") {
                        element("time", values.recordedAt)
                    }
                    element("trk") {
                        element("name", values.name)
                        element("trkseg") {
                            for (element in values.latLng.last()) {
                                element("trkpt") {
                                    attribute("lat", element.latlng.latitude.toString())
                                    attribute("lon", element.latlng.longitude.toString())
                                    element("ele", "0")

                                    element("time", element.time.toString())
                                }
                            }
                        }
                    }
                    Log.d("xml", "creating CPs")
                    Log.d("xml", values.checkpoints.toString())
                    for (element in values.checkpoints) {
                        Log.d("xml", element.toString())
                        element("wpt") {
                            attribute("lat", element.latitude.toString())
                            attribute("lon", element.longitude.toString())
                            element("ele", "0")
                            element("name", "CP")
                        }
                    }
                }
            }

            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val file = File(dir, "gpxsessionv2_" + values.id.toString() + ".xml")
            val fileWriter = FileWriter(file)
            fileWriter.append(gpx)
            fileWriter.flush()
            fileWriter.close()
        }
    }

    fun XmlSerializer.document(docName: String = "UTF-8", xmlStringWriter: StringWriter = StringWriter(), init: XmlSerializer.() -> Unit): String {
        startDocument(docName, true)
        xmlStringWriter.buffer.setLength(0) //  refreshing string writer due to reuse
        setOutput(xmlStringWriter)
        init()
        endDocument()
        return xmlStringWriter.toString()
    }

    fun XmlSerializer.element(name: String, init: XmlSerializer.() -> Unit) {
        startTag("", name)
        init()
        endTag("", name)
    }

    fun XmlSerializer.element(name: String,
                              content: String,
                              init: XmlSerializer.() -> Unit) {
        startTag("", name)
        init()
        text(content)
        endTag("", name)
    }

    fun XmlSerializer.element(name: String, content: String) =
        element(name) { text(content) }

    fun XmlSerializer.attribute(name: String, value: String) =
        attribute("", name, value)

    override fun getItemCount(): Int {
        return dataSet.count()
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)
}