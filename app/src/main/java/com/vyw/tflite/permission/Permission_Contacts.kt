package com.vyw.tflite.permission

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.vyw.tflite.MainActivity
import com.vyw.tflite.R

class Permission_Contacts : AppCompatActivity() {
    private val PERMISSIONS_REQUEST_CODE : Int = 101
    private val requiredPermissions = arrayOf(
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_CONTACTS
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts_permission)

        ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSIONS_REQUEST_CODE)

        val nextBtn = findViewById<Button>(R.id.nextButton_5)
        nextBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        /*val backBtn = findViewById<Button>(R.id.backButton_5)
        backBtn.setOnClickListener {
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            finish()
        }*/
    }
}