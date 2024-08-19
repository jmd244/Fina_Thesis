package com.vyw.tflite.maincomponent

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.drawable.Drawable
import android.location.Location
import android.media.MediaPlayer
import android.telephony.SmsManager
import android.util.Log
import android.view.View
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.vyw.tflite.databinding.ActivityCameraStarterBinding
import java.io.InputStream

/*
class ThresholdSensor {
    private var binder : ActivityCameraStarterBinding

    private var appContext: Context
    private var contactNum: String
    private var ringtone: Int
    private var volume: Float

    private var fusedLocationClient: FusedLocationProviderClient
    private var latitude: Double = 0.0
    private var longtitude: Double = 0.0


    private var alertSound = MediaPlayer()
    private var alertDesignation: Int = 0
    private var alertEmergency : Boolean = false

    constructor(
        context: Context,
        contact: String,
        ringtoneInx: Int,
        vol: Float,
        bind: ActivityCameraStarterBinding
    ) {
        appContext = context
        contactNum = contact
        ringtone = ringtoneInx + 1
        volume = (vol / 100)
        binder = bind
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        getLocation()
    }

    private var startTime = System.currentTimeMillis()
    private var alertStartTime: LongArray = longArrayOf(0L, 0L, 0L)

    private var counter: IntArray = intArrayOf(0, 0, 0);

    fun alert(alertType: IntArray): Int {
        //This settings is for EAR value
        if (alertType[0] == 0 && alertType[1] == 0 && alertType[2] == 0 && alertDesignation != 3) {
            cleanUpMediaPlayer()
        }
        Log.v(
            "CalibrationData",
            "Alert Type1: ${alertType[0]}, alertDesignation: $alertDesignation"
        )

        if (alertSound.isPlaying) {
            if(alertType[0] == 2){
                if (!alertEmergency){
                    cleanUpMediaPlayer()
                    alertEmergency = true
                    alertDesignation = 3
                    alertUser()
                }
            }
            return 1
        }else{
            if (alertEmergency){
                alertEmergency = false
            }
            cleanUpMediaPlayer()
        }

        if(!alertEmergency){
            if (alertType[0] == 1) {
                counter[0]++
                getLocation()
                if (alertStartTime[0] == 0L) {
                    alertStartTime[0] = System.currentTimeMillis()
                }
                alertDesignation = if (counter[0] < 2) {
                    1
                } else if (counter[0] < 4) {
                    2
                } else {
                    3
                }

                alertUser()

                Log.v("TimeAlert", "Time : ${getMin(getDuration(alertStartTime[0]))}")

                return 0
            }
            if (alertType[1] == 1) {
                return 0
            }
            if (alertType[2] == 1) {
                return 0
            }
        }

        return 0
    }

    private fun selectRingtone() {
        alertSound = MediaPlayer()
        val path = "Ringtones/set$ringtone"
        Log.d("selectRingtone", "$alertDesignation $ringtone")
        var descriptor: AssetFileDescriptor = appContext.assets.openFd("$path/a$alertDesignation.mp3")
        alertSound.setDataSource(
            descriptor.fileDescriptor,
            descriptor.startOffset,
            descriptor.length
        )
        descriptor.close()
    }

    private fun alertUser() {
        try{
            val inputStream : InputStream = appContext.assets.open("Images/w$alertDesignation.png")
            val drawable : Drawable? = Drawable.createFromStream(inputStream, null)
            drawable?.setBounds(0,0,50,50)
            inputStream.close()
            binder.signImage.setImageDrawable(drawable)
        }catch (e : Exception){

        }
        selectRingtone()
        alertSound.prepare()
        if (alertDesignation == 3) {
            sendSMS(contactNum, getLocation())
            alertSound.isLooping = true
            alertSound.setVolume(0.70f,0.70f)
            counter[0] = 0
        }
        binder.overlay.visibility = View.VISIBLE
        alertSound.setVolume(volume,volume)
        alertSound.start()
    }

    fun cleanUpMediaPlayer() {
        try {
            if (alertSound.isPlaying) {
                alertSound.stop()
                alertSound.release()
                alertSound = MediaPlayer()
            }
        } catch (e: Exception) {

        }
        alertDesignation = 0
        binder.overlay.visibility = View.GONE
    }

    private fun getMin(sec: Float): Float {
        val min = sec / 60
        return if (min >= 1) min else 0f
    }

    private fun getDuration(past: Long): Float {
        return (System.currentTimeMillis() - past).toFloat() / 100f
    }

    @SuppressLint("MissingPermission")
    private fun getLocation(): Array<Double> {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    latitude = location.latitude
                }
                if (location != null) {
                    longtitude = location.longitude
                }
            }
        Log.v(
            "GPSdata",
            "Latitude: $latitude, Longitude: $longtitude"
        )
        return arrayOf(latitude, longtitude)
    }

    private fun sendSMS(phoneNumber: String, coordinates: Array<Double>) {
        val message = "Alert Notice!\n\n" +
                "You are the chosen contact of [Name], " +
                "Plase do an Emergency action for the driver, " +
                "because the driver is currently sleepy" +
                "the coordinates of the driver at:\n" +
                "Latitude: ${coordinates[0]}, Longitude: ${coordinates[1]}"
        val smsManager = SmsManager.getDefault() as SmsManager
        smsManager.sendTextMessage(phoneNumber, null, message, null, null)
    }
}
*/

class ThresholdSensor {
    private var earThreshold: Float = 0f
    private var marThreshold: Float = 0f

    constructor(
        earAVG: Float,
        marAVG: Float
    ) {
        earThreshold = earAVG * 0.65f
    }

    fun alert(data: FloatArray): Int {
        // CODE 0 should indicate that there is no
        // CODE 1 should indicate an error which is no value
        val EAR = data[0]
//        Log.d("BlazeData", "EAR: ${data[0]} Threshold: $earThreshold")

        // If the earAVG and marAVG value is 0 it will return 1
        if (EAR == 0f && data[1] == 0f && data[0] == 0f) {
            return 1
        }

        // If EAR is less than the THRESHOLD it will return 2
        if (EAR < earThreshold) {
            return 2
        }

        return 0
    }
}