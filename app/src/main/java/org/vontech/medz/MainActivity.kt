package org.vontech.medz

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.otaliastudios.cameraview.*
import android.graphics.Color
import android.os.Handler
import android.view.View
import android.widget.ImageButton
import io.saeid.fabloading.LoadingView
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.AsyncTask
import android.support.v7.widget.CardView
import java.util.*
import android.view.animation.AnimationUtils
import android.view.animation.Animation
import android.widget.ImageView
import com.afollestad.materialdialogs.MaterialDialog
import com.shuhart.stepview.StepView


class MainActivity : AppCompatActivity() {

    private lateinit var cameraView: CameraView
    private lateinit var captureButton: ImageButton
    private lateinit var imageCapturingView: LoadingView
    private lateinit var scanStepper: StepView
    private lateinit var tutorialBox: CardView

    private val imageTakingHandler = Handler()
    private val processingHandler = Handler()
    private var stopImages = true
    var firstImage: String? = null
    var lastResult: Medication? = null

    private lateinit var prefs: MedPrefs

    private val textScanner = TextScan()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initCamera()
        loadMedicineLookup(this)
        prefs = MedPrefs(this)
    }

    private fun initCamera() {

        cameraView = findViewById(R.id.cameraView)
        cameraView.setLifecycleOwner(this)
        cameraView.mapGesture(Gesture.TAP, GestureAction.FOCUS_WITH_MARKER)
        cameraView.addCameraListener(object: CameraListener() {
            override fun onPictureTaken(jpeg: ByteArray?) {
                textScanner.processBytes(jpeg!!)

                // Save this image if it is the first image
                if (firstImage == null) {
                    val filename = "medz-${UUID.randomUUID()}"
                    this@MainActivity.openFileOutput(filename, Context.MODE_PRIVATE).use {
                        it.write(jpeg)
                    }
                    firstImage = filename
                }

            }
        })

        captureButton = findViewById(R.id.captureButton)
        captureButton.setOnClickListener {
            beginCapturing()
        }

        imageCapturingView = findViewById(R.id.imageCapturingView)
        imageCapturingView.addAnimation(Color.TRANSPARENT, R.drawable.pillsone, LoadingView.FROM_TOP)
        imageCapturingView.addAnimation(Color.TRANSPARENT, R.drawable.pillstwo, LoadingView.FROM_LEFT)
        imageCapturingView.addAnimation(Color.TRANSPARENT, R.drawable.pillsthree, LoadingView.FROM_BOTTOM)
        imageCapturingView.addAnimation(Color.TRANSPARENT, R.drawable.pillsfour, LoadingView.FROM_RIGHT)

        imageCapturingView.addListener(object : LoadingView.LoadingListener {
            override fun onAnimationStart(currentItemPosition: Int) {
                imageCapturingView.visibility = View.VISIBLE
            }

            override fun onAnimationRepeat(nextItemPosition: Int) {
                imageCapturingView.repeat = 2
            }

            override fun onAnimationEnd(nextItemPosition: Int) {
                imageCapturingView.visibility = View.GONE
            }
        })

        imageCapturingView.setOnClickListener {
            finishCapturing()
        }

        scanStepper = findViewById(R.id.scanProgress)
        scanStepper.setSteps(totalSteps)

        // Animate in the instructions
//        val imageView = findViewById<ImageView>(R.id.imageView)
//        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.your_fade_in_anim)
//        imageView.startAnimation(fadeInAnimation)
//        imageView.animation.setAnimationListener(object : Animation.AnimationListener {
//            override fun onAnimationRepeat(animation: Animation?) { }
//
//            override fun onAnimationEnd(animation: Animation?) { }
//
//            override fun onAnimationStart(animation: Animation?) {
//                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//            }
//
//        })

        tutorialBox = findViewById(R.id.tutorialBox)
        animateInTutorial()

    }

    private fun beginCapturing() {

        imageCapturingView.visibility = View.VISIBLE
        scanStepper.visibility = View.VISIBLE
        captureButton.visibility = View.GONE
        tutorialBox.visibility = View.GONE
        stopImages = false
        imageCapturingView.repeat = 2
        imageCapturingView.startAnimation()

        val runnable = object : Runnable {

            override fun run() {
                try {
                    cameraView.captureSnapshot()
                    //do your code here
                } catch (e: Exception) {
                    // TODO: handle exception
                } finally {
                    if (!stopImages) {
                        imageTakingHandler.postDelayed(this, 3000)
                    }
                }
            }
        }

        val parseRunnable = object : Runnable {

            override fun run() {
                try {

                    //do your code here
                } catch (e: Exception) {
                    // TODO: handle exception
                } finally {

                    // First, see what info we now have
                    val currentResults = getMedicationResult(textScanner)
                    updateStepper(currentResults)

                    if (!stopImages) {
                        processingHandler.postDelayed(this, 1000)
                    }
                }
            }
        }

        imageTakingHandler.post(runnable)
        processingHandler.post(parseRunnable)

    }

    private fun getFoundFields(med: Medication): Set<String> {
        val newResults = mutableSetOf<String>()
        med.derivativeNames?.apply { if (this.isNotEmpty()) newResults.add("Name") }
        med.expiration?.apply { newResults.add("Expir") }
        med.frequency?.apply { if (med.frequencyType != null) newResults.add("Freq") }
        med.dateFilled?.apply { newResults.add("Filled") }
        med.dosagePerUnit?.apply { if (med.dosageType != null) newResults.add("Dose")}
        med.phone?.apply { newResults.add("Phone") }
        med.quantity?.apply { newResults.add("Quant") }
        med.rxNumber?.apply { newResults.add("Rx") }
        return newResults
    }


    private var scanSteps = mutableListOf<String>()
    private var totalSteps = mutableListOf("Name", "Freq", "Dose", "Expir", "Filled", "Phone", "Quant", "Rx")
    private fun updateStepper(results: Medication) {

        val new = getFoundFields(results)
        val old = lastResult?.let { getFoundFields(it) } ?: mutableSetOf()
        val diff = new.subtract(old)

        if (diff.isNotEmpty()) {
            scanSteps.addAll(diff)
            scanStepper.setSteps(scanSteps + totalSteps.subtract(scanSteps))
            scanStepper.go(scanSteps.size - 1, true)
            scanStepper.done(true)
        }

        lastResult = results

    }

    private fun finishCapturing() {

        imageCapturingView.visibility = View.GONE
        captureButton.visibility = View.VISIBLE
        captureButton.setImageDrawable(resources.getDrawable(R.drawable.done, null))
        imageCapturingView.repeat = 1
        stopImages = true

        FinishScanningTask(textScanner, prefs, getLoadingDialog()).execute(this)

    }

    fun loadBitmapFromView(v: View): Bitmap {
        val b = Bitmap.createBitmap(v.layoutParams.width, v.layoutParams.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        v.layout(v.left, v.top, v.right, v.bottom)
        v.draw(c)
        return b
    }

    private fun getLoadingDialog(): MaterialDialog {
        return MaterialDialog.Builder(this)
                .title("Creating medication...")
                .customView(R.layout.loading_dialog, false)
                .build()
    }

    private fun animateInTutorial() {

        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_anim)
        fadeInAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) { }

            override fun onAnimationEnd(animation: Animation?) { }

            override fun onAnimationStart(animation: Animation?) {
                tutorialBox.visibility = View.VISIBLE
            }

        })
        println("STARTING ANIMATION")
        tutorialBox.startAnimation(fadeInAnimation)

    }



    override fun onResume() {
        super.onResume()
        cameraView.start()
    }

    override fun onPause() {
        super.onPause()
        cameraView.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraView.destroy()
    }
}

class FinishScanningTask(val textScanner: TextScan, val prefs: MedPrefs, val dialog: MaterialDialog): AsyncTask<MainActivity, Void, MainActivity>() {

    override fun onPreExecute() {
        super.onPreExecute()

        val view = dialog.customView
        val loadingView = view!!.findViewById<LoadingView>(R.id.loadingView)

        loadingView.addAnimation(Color.TRANSPARENT, R.drawable.pillsone, LoadingView.FROM_TOP)
        loadingView.addAnimation(Color.TRANSPARENT, R.drawable.pillstwo, LoadingView.FROM_LEFT)
        loadingView.addAnimation(Color.TRANSPARENT, R.drawable.pillsthree, LoadingView.FROM_BOTTOM)
        loadingView.addAnimation(Color.TRANSPARENT, R.drawable.pillsfour, LoadingView.FROM_RIGHT)

        loadingView.addListener(object : LoadingView.LoadingListener {
            override fun onAnimationStart(currentItemPosition: Int) {
                loadingView.visibility = View.VISIBLE
            }

            override fun onAnimationRepeat(nextItemPosition: Int) {
                loadingView.repeat = 2
            }

            override fun onAnimationEnd(nextItemPosition: Int) {
                loadingView.visibility = View.GONE
            }
        })

        loadingView.repeat = 2
        loadingView.startAnimation()
        dialog.show()
    }

    override fun doInBackground(vararg params: MainActivity): MainActivity {
        Thread.sleep(6000)
        return params[0]
    }

    override fun onPostExecute(context: MainActivity) {

        val currentResults = getMedicationResult(textScanner)

        // Set image path / id
        currentResults.imagePath = context.firstImage
        currentResults.medicineName = currentResults.derivativeNames?.first()

        val currentMeds = prefs.medications
        currentMeds.add(currentResults)
        prefs.medications = currentMeds

        val homePage = Intent(context, MedDetailActivity::class.java).apply {
            putExtra("MED", currentResults)
        }
        context.startActivity(homePage)

    }

}
