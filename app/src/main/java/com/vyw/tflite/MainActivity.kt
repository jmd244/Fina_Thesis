package com.vyw.tflite

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.vyw.tflite.databinding.ActivityMainBinding
import com.vyw.tflite.maincomponent.FaceCalibrator
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val PERMISSIONS_REQUEST_CODE = 101
    private val SPLASH_PREFS = "SplashPrefs"
    private val SPLASH_FLAG_KEY = "SplashFlag"
    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_CONTACTS
    )

    private var isAllPermissionTrue = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide();
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSIONS_REQUEST_CODE)
    }
    fun showSettingsDialog(list : String) {
        AlertDialog.Builder(this)
            .setTitle("Permission Denied")
            .setMessage("Required permissions has been denied. You can enable it from the app settings.\n$list")
            .setPositiveButton("Go to Settings") { _, _ ->
                // Redirect to app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var notGrantedList = ""
        permissions.forEach { permission ->
            when(permission){
                "android.permission.CAMERA" -> {
                    if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                        notGrantedList += "\n- Camera"
                    }
                }
                "android.permission.ACCESS_FINE_LOCATION" -> {
                    if(grantResults[1] != PackageManager.PERMISSION_GRANTED){
                        notGrantedList += "\n- GPS"
                    }
                }
                "android.permission.SEND_SMS" -> {
                    if (grantResults[2] != PackageManager.PERMISSION_GRANTED) {
                        notGrantedList += "\n- SMS"
                    }
                }
                "android.permission.READ_CONTACTS" -> {
                    if (grantResults[3] != PackageManager.PERMISSION_GRANTED) {
                        notGrantedList += "\n- Contacts"
                    }
                }
            }
        }
        isAllPermissionTrue = notGrantedList == ""
        if(!isAllPermissionTrue){
            showSettingsDialog(notGrantedList)
        }
    }

    fun checkPermission(){
        var notGrantedList = ""
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            notGrantedList += "\n- Camera"
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            notGrantedList += "\n- GPS"
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED){
            notGrantedList += "\n- SMS"
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED){
            notGrantedList += "\n- Contacts"
        }
        isAllPermissionTrue = notGrantedList == ""
        if(!isAllPermissionTrue){
            showSettingsDialog(notGrantedList)
        }
    }

    fun btnClick(view: View) {
//        val intent = Intent(this, Developer::class.java)
//        startActivity(intent)
        checkPermission()
        if(isAllPermissionTrue){
            val intent = Intent(this, FaceCalibrator::class.java)
            startActivity(intent)
        }
    }

    fun about_btnClick(view: View) {
        val intent = Intent(this, About::class.java)
        startActivity(intent)
    }

    fun settings_btnClick(view: View) {
        val intent = Intent(this, Settings_Main::class.java)
        startActivity(intent)
    }

    fun exit_click(view: View) {
        val mBuilder = AlertDialog.Builder(this)
            .setTitle("Exit")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { dialog, _ ->
                val prefs = getSharedPreferences(SPLASH_PREFS, Context.MODE_PRIVATE)
                prefs.edit().putBoolean(SPLASH_FLAG_KEY, true).apply()

                finish()
                exitProcess(-1)
            }
            .setNegativeButton("No", null)

        val mAlertDialog = mBuilder.create()
        mAlertDialog.show()

        val mNoButton = mAlertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
        mNoButton.setOnClickListener {
            mAlertDialog.cancel()
        }

    }
}
