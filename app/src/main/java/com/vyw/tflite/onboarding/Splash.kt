package com.vyw.tflite.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.vyw.tflite.MainActivity
import com.vyw.tflite.R
import com.vyw.tflite.maincomponent.FaceCalibrator

class splash : AppCompatActivity() {

    private val SPLASH_PREFS = "SplashPrefs"
    private val SPLASH_FLAG_KEY = "SplashFlag"
    private val permissions = mutableMapOf("Camera" to false, "GPS" to false, "Contacts" to false, "SMS" to false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            permissions["Camera"] = true
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED){
            permissions["SMS"] = true
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED){
            permissions["Contacts"] = true
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            permissions["GPS"] = true
        }

        val prefs = getSharedPreferences(SPLASH_PREFS, Context.MODE_PRIVATE)
        val splashFlag = prefs.getBoolean(SPLASH_FLAG_KEY, false)

        if (splashFlag) {
            startNextActivity()
        } else {
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                startNextActivity()
                prefs.edit().putBoolean(SPLASH_FLAG_KEY, true).apply()
            }, 3000)
        }
    }

    private fun startNextActivity(){
        val isAllPermissionTrue = permissions.all { it.value }
        if(isAllPermissionTrue){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }else{
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            finish()
        }

    }
}
