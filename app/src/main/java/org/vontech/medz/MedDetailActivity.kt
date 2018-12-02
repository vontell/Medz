package org.vontech.medz

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import java.io.File
import com.afollestad.materialdialogs.MaterialDialog
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelManager
import mehdi.sakout.fancybuttons.FancyButton
import org.json.JSONObject
import java.lang.Exception


class MedDetailActivity : AppCompatActivity() {

    private var medication: Medication? = null

    private lateinit var nameView: TextView
    private lateinit var imageView: ImageView
    private lateinit var frequencyView: TextView
    private lateinit var refillView: TextView
    private lateinit var rxNumView: TextView
    private lateinit var dosageView: TextView
    private lateinit var filledView: TextView
    private lateinit var phoneView: TextView
    private lateinit var dosageCont: View
    private lateinit var filledCont: View
    private lateinit var phoneCont: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_med_detail)

        // Grab each view
        nameView = findViewById(R.id.medName)
        imageView = findViewById(R.id.medImage)
        frequencyView = findViewById(R.id.medFreq)
        refillView = findViewById(R.id.medRefill)
        rxNumView = findViewById(R.id.rxNumber)
        dosageView = findViewById(R.id.dosageAmount)
        dosageCont = findViewById(R.id.dosageCont)
        filledView = findViewById(R.id.filledView)
        filledCont = findViewById(R.id.filledCont)
        phoneView = findViewById(R.id.phoneNumber)
        phoneCont = findViewById(R.id.phoneCont)

        if (intent.extras.containsKey("MED")) {
            medication = intent.extras.get("MED") as Medication
            println(medication)
            displayMedication(medication!!)
        }

        // Setup OpenFDA networking
        FuelManager.instance.basePath = "https://api.fda.gov/drug/label.json"

    }

    private fun displayMedication(med: Medication) {

        getFDAInfo(med, findViewById(R.id.instructionText), findViewById(R.id.warningText))

        if (med.medicineName != null) {
            nameView.text = med.medicineName
        } else if (med.derivativeNames != null && med.derivativeNames!!.isNotEmpty()) {
            nameView.text = med.derivativeNames!![0]
        } else {
            nameView.text = "Unknown Med"
        }

        nameView.setOnClickListener {
            displayMedNameChoiceDialog(med)
        }

        if (med.expiration != null) {
            refillView.text = "Expires on ${med.expiration}"
        } else {
            refillView.text = "Unknown expiration"
        }

        if (med.frequency == null || med.frequencyType == null) {
            frequencyView.text = "Unknown frequency"

        } else {
            frequencyView.text = "Take ${med.frequency} ${med.frequencyType.toString().toLowerCase()}"
        }

        if (med.rxNumber != null) {
            rxNumView.text = med.rxNumber
        } else {
            rxNumView.text = ""
        }

        if (med.dosageType != null && med.dosagePerUnit != null) {
            dosageView.text = "${med.dosagePerUnit} ${med.dosageType}"
        } else {
            dosageCont.visibility = View.GONE
        }

        if (med.phone != null) {
            phoneView.text = med.phone
        } else {
            phoneCont.visibility = View.GONE
        }

        if (med.dateFilled != null) {
            filledView.text = med.dateFilled
        } else {
            filledCont.visibility = View.GONE
        }


        // Load image
        val file = File(filesDir, med.imagePath)
        val d = Drawable.createFromPath(file.path) as BitmapDrawable
        imageView.setImageDrawable(d)

        findViewById<FancyButton>(R.id.deletePrescriptionButton).setOnClickListener {
            MaterialDialog.Builder(this)
                    .title("Delete Prescription?")
                    .content("Are you sure you want to delete this medication?")
                    .positiveText("Yes")
                    .negativeText("No")
                    .onPositive { dialog, which ->
                        val oldMeds = MedPrefs(this).medications
                        oldMeds.remove(med)
                        MedPrefs(this).medications = oldMeds
                        val intent = Intent(this, OverviewActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    .onNegative { dialog, which ->
                        dialog.cancel()
                    }.show()
        }

        findViewById<FancyButton>(R.id.createAlarmButton).setOnClickListener {
            MaterialDialog.Builder(this)
                    .title("Create Alarm")
                    .content("This medication requests you to ${frequencyView.text.toString().toLowerCase()}. Create an alarm for this medication?")
                    .positiveText("Create Alarms")
                    .negativeText("Cancel")
                    .onPositive { dialog, which ->
                        dialog.cancel()

                        MaterialDialog.Builder(this)
                                .title("Alarm Created")
                                .content("Alarm for ${med.medicineName} has been created!")
                                .positiveText("Ok!")
                                .show()

                    }
                    .onNegative { dialog, which ->
                        dialog.cancel()
                    }.show()
        }

    }

    private fun displayMedNameChoiceDialog(med: Medication) {
        MaterialDialog.Builder(this)
                .title("Choose Medication")
                .items(med.derivativeNames!!.toSet())
                .itemsCallback { dialog, view, which, text ->

                    val oldMeds = MedPrefs(this).medications
                    oldMeds.remove(med)

                    med.medicineName = text.toString()
                    displayMedication(med)
                    oldMeds.add(med)

                    MedPrefs(this).medications = oldMeds
                    dialog.cancel()
                }
                .show()
    }

    private fun getFDAInfo(med: Medication, instructionText: TextView, warningText: TextView) {

        println("https://api.fda.gov/drug/label.json?search=openfda.generic_name:\"${med.medicineName}\"")
        Fuel.get("https://api.fda.gov/drug/label.json?search=openfda.generic_name:\"${med.medicineName}\"").response { request, response, result ->
            runOnUiThread {
                if (response.statusCode == 200) {
                    val jObj = JSONObject(String(response.data))
                    println(jObj.toString())
                    try {
                        val desc = jObj.getJSONArray("results").getJSONObject(0).getJSONArray("description").getString(0)
                        instructionText.text = desc
                    } catch (e: Exception) {
                        e.printStackTrace()
                        instructionText.text = "No instructions found"
                    }

                    try {
                        val warning = jObj.getJSONArray("results").getJSONObject(0).getJSONArray("warnings").getString(0)
                        warningText.text = warning
                    } catch (e: Exception) {
                        warningText.text = "No warnings found"
                    }
                } else {
                    instructionText.text = "No instructions found"
                    warningText.text = "No warnings found"
                }
            }

        }

    }

}
