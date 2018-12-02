package org.vontech.medz

import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.opencsv.CSVReader
import java.io.Serializable


/**
 * Representation of a medication
 */
data class Medication(
        var medicineName: String? = null,
        var derivativeNames: List<String>? = null,
        var quantity: String? = null,
        var dosagePerUnit: Float? = null,
        var dosageType: DosageType? = null,
        var frequency: Int? = null,
        var frequencyType: FrequencyType? = null,
        var specialInstructions: String? = null,
        var dateFilled: String? = null,
        var expiration: String? = null,
        var phone: String? = null,
        var rxNumber: String? = null,
        var imagePath: String? = null
): Serializable

enum class FrequencyType {
    DAILY, MONTHLY, HOURLY
}

enum class DosageType {
    MCG, IU
}

fun getMedicationResult(textScan: TextScan): Medication {

    val toSearch = textScan.getStitchedResult()

    var dosagePerUnit: Float? = null
    var dosageType: DosageType? = null
    findInString(DOSAGE_REGEX, toSearch)?.groups?.let {
        dosagePerUnit = it[1]?.value?.toFloat()
        dosageType = DosageType.valueOf(it[2]?.value!!.toUpperCase())
    }

    var quantity: String? = null
    findInString(QTY_REGEX, toSearch)?.groups?.let {
        quantity = it[1]?.value
    }

    var freqType: FrequencyType? = null
    findInString(FREQ_TYPE, toSearch)?.groups?.let {
        freqType = FrequencyType.valueOf(it[1]?.value!!.toUpperCase())
    }

    var freq: Int? = null
    findInString(FREQ_REGEX, toSearch)?.groups?.let {
        freq = if (it.size > 2) {
            it[2]!!.value.trim(')').trim('(').toInt()
        } else {
            val s = it[1]!!.value.toLowerCase()
            if (NUMERALS.containsKey(s)) {
                NUMERALS[s]
            } else {
                s.toInt()
            }
        }
    }

    var rxNumber: String? = null
    findInString(RX_NUM_REGEX, toSearch)?.groups?.let {
        rxNumber = it[1]?.value!!
    }

    var phoneNumber: String? = null
    findInString(PHONE_REGEX, toSearch)?.groups?.let {
        phoneNumber = it[1]?.value!!
    }

    var filled: String? = null
    findInString(FILLED_REGEX, toSearch)?.groups?.let {
        filled = it[1]?.value!!
    }

    var expires: String? = null
    findInString(EXPIRE_REGEX, toSearch)?.groups?.let {
        expires = it[1]?.value!!
    }

    // Next, search for the name. This is done by the following:
    //  Ordering token in order of size (bounding box) and then confidence
    //  Find all generic names that are potential candidates
    //  Create score for each name
    //  Choose or recommend top score items
    val elements = textScan.elements.flatten().map { it.elements }.flatten().sortedBy { -1.0 * (it.confidence?: 0f) }
    var possibleNames = mutableListOf<String>()
    elements.forEach {
        val c = it.text.toLowerCase()
        if (c in MEDICINE_LOOKUP!!) {
            possibleNames.add(MEDICINE_LOOKUP!![c]!!.first)
        }
    }

    return Medication(
            quantity = quantity,
            dosagePerUnit = dosagePerUnit,
            dosageType = dosageType,
            frequencyType = freqType,
            frequency = freq,
            rxNumber=rxNumber,
            phone = phoneNumber,
            dateFilled = filled,
            expiration = expires,
            derivativeNames = possibleNames
    )

}

val ITEM_TYPES = listOf("tablets", "softgels", "capsules")
val NUMERALS = mapOf("one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9)
val TAKE_ACTIONS = listOf("take", "swallow")

val TAKE_JOINED = TAKE_ACTIONS.joinToString("|")
val NUM_JOINED = NUMERALS.keys.joinToString("|")

val DOSAGE_REGEX = "(\\d+)\\s*(${DosageType.values().joinToString("|")})"
val QTY_REGEX_ONE = "(\\d+ (?:${ITEM_TYPES.joinToString("|")}))"
val QTY_REGEX_TWO = "(qty|qtv)\\s*:{0,1}\\s*(\\d+)"
val QTY_REGEX = "(?:$QTY_REGEX_ONE|$QTY_REGEX_TWO)"

val FREQ_REGEX = "(:?$TAKE_JOINED){0,1} ($NUM_JOINED|\\d+) (\\(\\d+\\)){0,1}"
val FREQ_TYPE = "(${FrequencyType.values().joinToString("|")})"

val PHONE_REGEX = "(\\({0,1}\\d{3}\\){0,1}\\s*\\d{3}-\\d{4})"

val RX_NUM_REGEX = "Rx\\s*(#\\s*\\d{7})"

val FILLED_REGEX = "filled\\s*:{0,1}\\s*(\\d{2}/\\d{2}/\\d{4})"
val EXPIRE_REGEX = "after\\s*:{0,1}\\s*(\\d{2}/\\d{2}/\\d{4})"

//var MEDICINE_LOOKUP : MutableList<Pair<String, String>>? = null
var MEDICINE_LOOKUP : HashMap<String, Pair<String, String>>? = null

fun loadMedicineLookup(context: Context) {
    if (MEDICINE_LOOKUP == null) {
        LoadMedicineAsync().execute(context)
    }
}

class LoadMedicineAsync: AsyncTask<Context, Void, Void>() {

    override fun doInBackground(vararg params: Context?): Void? {
        val context = params[0]!!
        println("LOADING MEDICINE LIST")
        val am = context.assets
        //val input = SplitFileInputStream("medicines", ".txt", 10, am)
        val input = am.open("medicines.txt")
        val csvReader = CSVReader(input.reader())
        var nextLine = csvReader.readNext()
        MEDICINE_LOOKUP = HashMap()
        while (nextLine != null) {
            (nextLine[0].toLowerCase().split("\\s*") + nextLine[0].toLowerCase().split("\\s*")).forEach {
                MEDICINE_LOOKUP!![it] = Pair(nextLine[0], nextLine[1])
            }
            nextLine = csvReader.readNext()
        }
        println("FINISHED LOADING ${MEDICINE_LOOKUP!!.size} MEDICINES")
        input.close()
        csvReader.close()
        return null
    }

    override fun onPostExecute(result: Void?) {
        super.onPostExecute(result)
    }

}

val gson = GsonBuilder().setPrettyPrinting().create()
class MedPrefs (context: Context) {
    private val PREFS_FILENAME = "org.vontech.medz.prefs"
    private val MEDZ_LIST = "medz_list"
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)

    var medications: MutableList<Medication>
        get() = gson.fromJson(prefs.getString(MEDZ_LIST, "[]"), object : TypeToken<List<Medication>>() {}.type)
        set(value) = prefs.edit().putString(MEDZ_LIST, gson.toJson(value)).apply()
}

//class MedPrefs (context: Context) {
//    private val PREFS_FILENAME = "org.vontech.medz.prefs"
//    private val MEDZ_LIST = "medz_list"
//    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
//
//    var medications: MutableList<Medication>
//        get() = mutableListOf()
//        set(value) = {}()
//}