package com.vyw.tflite

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.vyw.tflite.algorithm.BlazeFaceNcnn
import com.vyw.tflite.databinding.ActivityCameraStarterBinding
import com.vyw.tflite.maincomponent.Calibration
import com.vyw.tflite.maincomponent.SettingsPref
import com.vyw.tflite.maincomponent.ThreadPool
import com.vyw.tflite.maincomponent.ThresholdSensor
import java.lang.NullPointerException
import java.time.Instant

class CameraStarter : Activity(), SurfaceHolder.Callback {
    private lateinit var binding: ActivityCameraStarterBinding
    private lateinit var dialogBinding: View
    private lateinit var settings: SettingsPref
    private lateinit var calibration: Calibration
    private var mediaPlayer: MediaPlayer? = null
    private val REQUEST_CAMERA = 100

    private val locationPermissionCode = 2

    private var blazefacecnn = BlazeFaceNcnn()
    private val facing = 0
    private var currentModel = 0
    private var currentCPUGPU = 0
    private var draw = true

    @Volatile
    private var isCameraOpen: Boolean = false

    @Volatile
    private lateinit var sensor: ThresholdSensor


    // alertStartTime used as a value for the initial start of EAR and MAR scanning
    @RequiresApi(Build.VERSION_CODES.O)
    @Volatile
    private  var alertStartTime = Instant.now().toEpochMilli()

    // alertIntervalStartTime initially has a value of start time but it will change every 5 minutes
    @RequiresApi(Build.VERSION_CODES.O)
    @Volatile
    private var alertIntervalStartTime = alertStartTime

    @Volatile
    private var isNeedAAttention = 0

    @Volatile private var MARcalibrated = 0f

    @Volatile private var EARcalibrated = 0f
    @Volatile private var EARthresholdTimeCounter = 0
    @Volatile private var alert1Counter = 0
    @Volatile private var alertPresentTime = 0L
    @Volatile private var alertIntervalTime = 0L

    @Volatile private lateinit var dialog : Dialog



    private val threadPool = ThreadPool()


    // This function serves as a sound for the alert
    private fun Context.playSound(alertType: String, index : Int) {
        if (mediaPlayer?.isPlaying == true) {
            Log.d("Media", "Media is playing!")
        } else {
            var resourceID = 0
            when (alertType) {
                "EAR" -> {
                    when (index) {
                        1 -> resourceID = R.raw.e1
                        2 -> resourceID = R.raw.e2
                        3 -> resourceID = R.raw.e3
                    }
                }
                "No Face" -> resourceID = R.raw.nf
            }
            mediaPlayer = MediaPlayer.create(this, resourceID)
            mediaPlayer!!.start()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private val alert = Runnable {
        while (isCameraOpen) {
            if (isNeedAAttention == 0) {
                lateinit var data : FloatArray
                try {
                    // alertType is used to get the data gathered information EAR and MAR value
                    data = blazefacecnn.data()
                    val ear = data[0]
                    val mar = data[1]

                    alertPresentTime = Instant.now().toEpochMilli()

                    earSensor(ear)
                    marSensor(mar)


                    alertIntervalTime = (alertPresentTime - alertIntervalStartTime) / 1000

                    if(alertIntervalTime >= 180){
                        alertIntervalStartTime = alertPresentTime
                        EARthresholdTimeCounter = 0
                        Log.d(
                            "Alert",
                            "Alert time restarted"
                        )
                    }
                } catch (e: NullPointerException) {
                    Log.d("NullDetector", "No Face")
                }
            } else {
                runOnUiThread{
                    if(!dialog.isShowing){
                        makeDialog(R.layout.dialog_alert3)
                    }
                }
                playSound("EAR",3)
            }

            try {
                Thread.sleep(1000)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraStarterBinding.inflate(layoutInflater)

//        if(calibration.ear.isEmpty()){
//            val intent = Intent(this, Calibrate::class.java)
//            startActivity(intent)
//        }
        settings = SettingsPref(this)
        val calibrateBundle = intent.getBundleExtra("data")
        val earAVG = calibrateBundle!!.getFloat("earAVG")
        EARcalibrated = calibrateBundle.getFloat("earAVG")
        val marAVG = calibrateBundle.getFloat("marAVG")
        MARcalibrated = calibrateBundle.getFloat("marAVG")
        var pref = settings.getPreference()
        sensor = settings.getSettings()?.let {
            ThresholdSensor(
                earAVG,
                marAVG
            )
        }!!
        calibration = Calibration(
            earAVG = calibrateBundle.getFloat("earAVG"),
            headPoseAVG = 0f,
            rectAVG = calibrateBundle.getFloat("rectAVG")
        )
        Log.v("CalibrationData", "earAVG : ${calibration.earAVG}")
        setContentView(binding.root)

        blazefacecnn.initiateCamera(calibration.earAVG.toFloat(), calibration.rectAVG.toFloat())

        binding.cameraview.holder.setFormat(PixelFormat.RGBA_8888)
        binding.cameraview.holder.addCallback(this)

//        binding.isdraw.setOnCheckedChangeListener { _, isChecked ->
//            draw = isChecked
//            reload()
//        }
        binding.stopButton.setOnClickListener {
//            sensor.cleanUpMediaPlayer()
        }

        reload()
    }

    private fun reload() {
        val retInit: Boolean = blazefacecnn.loadModel(assets, currentModel, currentCPUGPU, draw)
        if (!retInit) {
            Log.e("MainActivity", "blazefacecnn loadModel failed")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun surfaceCreated(holder: SurfaceHolder) {
        isCameraOpen = blazefacecnn.openCamera(facing)
        threadPool.execute(alert)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        holder.let {
            Log.d("HatdogCallback", "running")
        }
        blazefacecnn.setOutputWindow(holder.surface)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isCameraOpen = blazefacecnn.closeCamera()
        Log.d("HatdogCallback", "doghat destroyed")
    }

    //Overrides Activity() function
    override fun onResume() {
        super.onResume()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isCameraOpen = blazefacecnn.closeCamera()
    }

    fun back_click(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        //super.onBackPressed()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun makeDialog(layout : Int) {
        dialogBinding = layoutInflater.inflate(layout, null)
        dialog = Dialog(this)
        dialog.setContentView(dialogBinding)

        if(layout == R.layout.dialog_alert3){
            Toast.makeText(this, "Alert 3 activated", Toast.LENGTH_SHORT).show()
            val closeBtn = dialog.findViewById<Button>(R.id.closeBtn)
            closeBtn.setOnClickListener {
                isNeedAAttention = 0
                alert1Counter = 0
                dialog.dismiss()
            }
        }

        dialog.window?.setDimAmount(0F)
        dialog.window?.setWindowAnimations(R.style.CustomDialogTheme)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))



        dialog.show()
    }

    fun marSensor(marAVG: Float) {
        val marThreshold = 0.60f
        Log.d("MAR sensor", "MARavg = $marAVG, threshold = $marThreshold")
        if(marAVG >= marThreshold){
            Log.w("MAR sensor", "Yawning detected!")
        }
    }

    fun earSensor(earAVG: Float){
        val earThreshold = EARcalibrated * 0.65f
        Log.d(
            "EAR sensor",
            "EARavg = $earAVG, threshold = $earThreshold"
        )
        if(earAVG == 0f){
            playSound("No Face",1)
            EARthresholdTimeCounter =  0
            if(::dialog.isInitialized){
                runOnUiThread{
                    dialog.dismiss()
                }
            }
        }
        if (earAVG < earThreshold) {
            EARthresholdTimeCounter += 1
        } else {
            EARthresholdTimeCounter = 0
        }

        if (EARthresholdTimeCounter == 0) {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer!!.stop()
            }
            if(::dialog.isInitialized){
                runOnUiThread{
                    dialog.dismiss()
                }
            }
        } else if (EARthresholdTimeCounter in 2..2) {
            Log.d("BlazeIndicator", "Medium alert!")
            alert1Counter++
            playSound("EAR",2)
            runOnUiThread{
                makeDialog(R.layout.dialog_alert2)
            }
        } else if (EARthresholdTimeCounter > 4 || alert1Counter > 4) {
            Log.d("BlazeIndicator", "The driver needs to stop over and rest")
            isNeedAAttention = 1
            mediaPlayer!!.stop()
            dialog.dismiss()
        }

    }
}
