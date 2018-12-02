package org.vontech.medz

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import mehdi.sakout.fancybuttons.FancyButton
import java.io.File
import android.graphics.drawable.Drawable
import android.widget.ImageView
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream


/**
 * Contains the primary code for displaying prescription information and
 * providing buttons for managing these prescriptions.
 */
class OverviewActivity : AppCompatActivity() {

    private lateinit var medicationListView: ListView
    private lateinit var medications: List<Medication>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_overview)

        val newPrescriptionButton = findViewById<FancyButton>(R.id.newPrescriptionButton)
        medicationListView = findViewById(R.id.medicationListView)

        newPrescriptionButton.iconImageObject.layoutParams.height = 180
        newPrescriptionButton.iconImageObject.layoutParams.width = 180
        newPrescriptionButton.iconImageObject.requestLayout()

        newPrescriptionButton.setOnClickListener {
            val cameraIntent = Intent(this, MainActivity::class.java)
            startActivity(cameraIntent)
        }

        initListOfMeds()

        loadMedicineLookup(this)

    }

    private fun initListOfMeds() {
        medications = MedPrefs(this).medications
        val adapter = MedicationAdapter(this, medications)
        medicationListView.adapter = adapter

        println(medications)

        if (medications.isEmpty()) {
            findViewById<ImageView>(R.id.header).visibility = View.VISIBLE
            findViewById<TextView>(R.id.headerText).visibility = View.VISIBLE
        } else {
            findViewById<ImageView>(R.id.header).visibility = View.GONE
            findViewById<TextView>(R.id.headerText).visibility = View.GONE
        }


    }

    private fun clearMeds() {
        MedPrefs(this@OverviewActivity).medications = mutableListOf()
        medicationListView.invalidate()
    }

}

class MedicationAdapter(private val context: Context,
                        private val dataSource: List<Medication>) : BaseAdapter() {

    private val inflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return dataSource.size
    }

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Get view for row item

        var tempView = convertView
        if (tempView == null) {
            tempView = inflater.inflate(R.layout.med_overview, parent, false)
        }
        val rowView = tempView!!

        val med = dataSource[position]

        rowView.findViewById<TextView>(R.id.medName).text = (med.medicineName ?: med.derivativeNames?.get(0)?: "Unknown Rx")
        rowView.findViewById<TextView>(R.id.medFreq).text = "Take ${med.frequency} ${med.frequencyType.toString().toLowerCase()}"
        if (med.expiration != null) {
            rowView.findViewById<TextView>(R.id.medRefill).text = "Expires on ${med.expiration}"
        }


        // Load image
        val image = rowView.findViewById<ImageView>(R.id.medImage)
        if (med.imagePath != null) {
            val directory = context.filesDir
            val file = File(directory, med.imagePath)
            val d = Drawable.createFromPath(file.path) as BitmapDrawable

            val out = ByteArrayOutputStream()
            d.bitmap.compress(Bitmap.CompressFormat.JPEG, 10, out)
            val decoded = BitmapFactory.decodeStream(ByteArrayInputStream(out.toByteArray()))

            image.setImageBitmap(decoded)
            image.visibility = View.VISIBLE
        } else {
            image.visibility = View.GONE
        }

        rowView.setOnClickListener {
            val intent = Intent(context, MedDetailActivity::class.java).apply {
                putExtra("MED", med)
            }
            context.startActivity(intent)
        }

        rowView.setOnLongClickListener {
            var text = "This is ${med.medicineName}. "
            if (med.frequency != null && med.frequencyType != null) {
                text += "You should take ${med.frequency} ${med.frequencyType}. "
            }
            if (med.expiration != null) {
                text += "Make sure to get a refill by ${med.expiration}. "
            }
            println(text)
            rowView.announceForAccessibility(text)
            true
        }

        return rowView
    }

}
