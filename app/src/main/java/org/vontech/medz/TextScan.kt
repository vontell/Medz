package org.vontech.medz

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText

/**
 * Created by vontell on 11/30/18.
 */
class TextScan {

    val scannedTexts = mutableListOf<String>()
    val elements = mutableListOf<List<FirebaseVisionText.Line>>()

    fun processBytes(bytes: ByteArray) {
        val backgroundTask = ScanProcessAsyncTask(this)
        backgroundTask.execute(bytes)
    }

    fun handleUpdate() {
        println("-----------------------------------")
        //scannedTexts.forEach { println(it) }
        println(scannedTexts.last())
    }

    fun getStitchedResult(): String {

        // Algorithm description:
        // - Designate first image as the base
        // - (Loop)
        //      - For each line in next image, find first instance where a token is in base

        return scannedTexts.joinToString("\n")

    }

}

class ScanProcessAsyncTask(private val textScanner: TextScan): AsyncTask<ByteArray, Void, Bitmap>() {

    override fun doInBackground(vararg jpegs: ByteArray): Bitmap {

        // Convert to Bitmap
        val jpeg = jpegs[0]
        return BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)

    }

    override fun onPostExecute(result: Bitmap) {

        // Process with firebase
        val image = FirebaseVisionImage.fromBitmap(result)
        val recognizer = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer()

        recognizer.processImage(image)
                .addOnSuccessListener { texts ->
                    texts.textBlocks.forEach {
                        textScanner.elements.add(it.lines.toList())
                    }
                    textScanner.scannedTexts.add(texts.text)
                    textScanner.handleUpdate()

                    // Due to memory issue, recycle the drawable
                    result.recycle()

                }
                .addOnFailureListener { e ->
                    // Task failed with an exception
                    e.printStackTrace()
                }

    }
}