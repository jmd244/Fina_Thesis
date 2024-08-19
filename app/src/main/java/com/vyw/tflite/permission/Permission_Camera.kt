package com.vyw.tflite.permission

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.core.app.ActivityCompat
import com.vyw.tflite.MainActivity
import com.vyw.tflite.R

class Permission_Camera : AppCompatActivity() {
    private val REQUEST_CAMERA = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_permission)

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA)
        val nextBtn = findViewById<Button>(R.id.nextButton_4)
        nextBtn.setOnClickListener {
            val intent = Intent(this, Permission_Contacts::class.java)
            startActivity(intent)
            finish()
        }

        val backBtn = findViewById<Button>(R.id.backButton_4)
        backBtn.setOnClickListener {
            val intent = Intent(this, Permission_Location::class.java)
            startActivity(intent)
            finish()
        }
    }
}