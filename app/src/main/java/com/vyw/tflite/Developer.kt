package com.vyw.tflite

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.location.LocationManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.vyw.tflite.algorithm.BlazeFaceNcnn
import com.vyw.tflite.databinding.ActivityDeveloperBinding
import com.vyw.tflite.maincomponent.Calibration
import com.vyw.tflite.maincomponent.SettingsPref
import com.vyw.tflite.maincomponent.ThreadPool
import com.vyw.tflite.maincomponent.ThresholdSensor
import java.lang.NullPointerException
import java.time.Instant

class Developer : AppCompatActivity(), SurfaceHolder.Callback {
    private lateinit var binding: ActivityDeveloperBinding
    private lateinit var dialogBinding: View
    private lateinit var settings: SettingsPref
    private lateinit var calibration: Calibration
    private var mediaPlayer: MediaPlayer? = null
    private var volume: Int = 0
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
    private var alertIntervalStartTime = 0L

    @Volatile private var isNeedAAttention = 0
    @Volatile private var alertType = ""

    @Volatile private var MARcalibrated = 0f
    @Volatile private var MARthresholdTimeCounter = 0
    @Volatile private var MARalertStartTime = 0

    @Volatile private var EARcalibrated = 0f
    @Volatile private var EARThreshold = 0f
    @Volatile private var EARthresholdTimeCounter = 0
    @Volatile private var alert1Counter = 0
    @Volatile private var alertPresentTime = 0L
    @Volatile private var alertIntervalTime = 0L

    @Volatile private var dialog : Dialog? = null

    // This function serves as a sound for the alert
    private fun playSound(alert: String) : Int {
        runOnUiThread {
            if(mediaPlayer == null) {
                when (alert) {
                    "No Face" -> {
                        mediaPlayer = MediaPlayer.create(this, R.raw.nf)
                    }
                    "MAR" -> {
                        mediaPlayer = MediaPlayer.create(this, R.raw.e1)
                    }
                    "EAR" -> {
                        mediaPlayer = MediaPlayer.create(this, R.raw.e2)
                    }
                    "Critical" -> {
                        mediaPlayer = MediaPlayer.create(this, R.raw.e3)
                    }
                }

                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val newVolume = volume.coerceIn(0, maxVolume)
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    newVolume,
                    0
                )
                mediaPlayer?.setVolume(volume.toFloat()/100, volume.toFloat()/100)
                mediaPlayer?.start()
            }
        }
        return 0
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private val Blink_monitor = Runnable{
        while (isCameraOpen){
            if(isNeedAAttention == 0){

            }
            try {
                Thread.sleep(100)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private val EARMAR_monitor = Runnable {
        alertStartTime = Instant.now().toEpochMilli()
        alertIntervalStartTime = alertStartTime
        var monitorDuration = alertStartTime
        while (isCameraOpen) {
            if (isNeedAAttention == 0) {
                lateinit var data : FloatArray
                try {
                    // alertType is used to get the data gathered information EAR and MAR value
                    data = blazefacecnn.data()
                    val ear = data[0]
                    val mar = data[1]
                    val fps = data[2]
                    val fdfls_ms = data[3]

                    runOnUiThread{
                        binding.fps.text = "FPS: $fps"
                        binding.fdflms.text = "FDFL ms: $fdfls_ms"
                        binding.ear.text = "EAR: $ear"
                        binding.mar.text = "MAR: $mar"
                    }

                    alertPresentTime = Instant.now().toEpochMilli()

                    alertType = earSensor(ear)
                    if(alertType.isEmpty()){
                        alertType = marSensor(mar)
                    }

                    when(alertType){
                        "EAR" -> {
                            makeDialog(R.layout.dialog_alert2)
                            playSound(alertType)
                        }
                        "MAR" -> {
                            makeDialog(R.layout.dialog_alert1)
                            playSound(alertType)
                        }
                        "No Face" -> {
                            playSound(alertType)
                            EARthresholdTimeCounter =  0
                        }
                        else -> {
                            dialog_purge()
                        }
                    }

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
                if(dialog==null){
                    makeDialog(R.layout.dialog_alert3)
                }
                playSound("Critical")
                alertType = "Critical"
            }

            try {
                Thread.sleep(1000)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            monitorDuration = (Instant.now().toEpochMilli() - alertStartTime)/1000
            runOnUiThread {
                binding.time.text = "Time: $monitorDuration"
            }
        }
    }

    private val threadPool = ThreadPool()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_developer)
        binding = ActivityDeveloperBinding.inflate(layoutInflater)

        settings = SettingsPref(this)
        volume = settings.getVolume()
        val calibrateBundle = intent.getBundleExtra("data")
        val earAVG = calibrateBundle!!.getFloat("earAVG")
        EARcalibrated = calibrateBundle.getFloat("earAVG")
        EARThreshold = EARcalibrated * 0.80f
        binding.thrs.text = "Threshold: $EARThreshold"
        val marAVG = calibrateBundle.getFloat("marAVG")
        MARcalibrated = calibrateBundle.getFloat("marAVG")
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

        blazefacecnn.initiateCamera()

        binding.cameraview.holder.setFormat(PixelFormat.RGBA_8888)
        binding.cameraview.holder.addCallback(this)

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
        threadPool.execute(EARMAR_monitor)
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
        finish()
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

    @SuppressLint("MissingPermission")
    fun sendSMS() {
        runOnUiThread{
            val phoneNumber = settings.getContactNumber()
            if (phoneNumber != null) {
                val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                var location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (location == null) {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                }
                val latitude = location!!.latitude
                val longitude = location.longitude
                Toast.makeText(this, "$latitude, $longitude", Toast.LENGTH_SHORT).show()

                val smsManager: SmsManager = SmsManager.getDefault()
                val message = "Alert!"
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            }else {
                Toast.makeText(this, "No contact number", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun makeDialog(layout : Int) {
        runOnUiThread {
            if(dialog == null) {
                dialogBinding = layoutInflater.inflate(layout, null)
                dialog = Dialog(this)
                dialog!!.setContentView(dialogBinding)

                dialog!!.window?.setDimAmount(0F)
                dialog!!.window?.setWindowAnimations(R.style.CustomDialogTheme)
                dialog!!.setCancelable(false)
                dialog!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                dialog!!.show()
                if(layout == R.layout.dialog_alert3){
                    val closeBtn = dialog!!.findViewById<Button>(R.id.closeBtn)
                    closeBtn.setOnClickListener {
                        isNeedAAttention = 0
                        alert1Counter = 0
                        dialog!!.dismiss()
                    }
                }
            }
        }
    }

    fun dialog_purge(){
        runOnUiThread{
            if(mediaPlayer != null){
                if (mediaPlayer!!.isPlaying) {
                    mediaPlayer!!.stop()
                    mediaPlayer!!.release()
                    mediaPlayer = null
                }
            }
            if(dialog != null){
                dialog!!.hide()
                dialog!!.dismiss()
                dialog = null
            }
        }
        alertType = ""
    }

    fun marSensor(marAVG: Float) : String{
        val marThreshold = 0.60f
        Log.d("MAR sensor", "MARavg = $marAVG, threshold = $marThreshold")
        if(marAVG >= marThreshold){
            Log.w("MAR sensor", "Yawning detected!")
            alertType = "MAR"
            return alertType
        }else{
            dialog_purge()
            return ""
        }
    }

    fun earSensor(earAVG: Float) : String{
        Log.d(
            "EAR sensor",
            "EARavg = $earAVG, threshold = $EARThreshold"
        )
        if(earAVG == 0f){
            alertType = "No Face"
            return alertType
        }
        if (earAVG < EARThreshold) {
            EARthresholdTimeCounter += 1
            Log.d("EAR sensor", "Counter = $EARthresholdTimeCounter")
            if (EARthresholdTimeCounter in 2..3) {
                Log.d("BlazeIndicator", "Medium alert!")
                alert1Counter++
                alertType = "EAR"
                return alertType
            } else if (EARthresholdTimeCounter > 4 || alert1Counter > 4) {
                Log.d("BlazeIndicator", "The driver needs to stop over and rest")
                isNeedAAttention = 1
                alertType = "Critical"
                return alertType
            }
        } else {
            EARthresholdTimeCounter = 0
            alertType = ""
            return alertType
        }

        return ""
    }
}