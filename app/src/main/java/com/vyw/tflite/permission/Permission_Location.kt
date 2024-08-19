package com.vyw.tflite.permission

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import com.vyw.tflite.R

class Permission_Location : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_permission)

        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    // Precise location access granted.
                }
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    // Only approximate location access granted.
                } else -> {
                // No location access granted.
            }
            }
        }
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))

        val nextBtn = findViewById<Button>(R.id.nextButton_3)
        nextBtn.setOnClickListener {
            val intent = Intent(this, Permission_Camera::class.java)
            startActivity(intent)
            finish()
        }

        val backBtn = findViewById<Button>(R.id.backButton_3)
        backBtn.setOnClickListener {
            val intent = Intent(this, camera_placement::class.java)
            startActivity(intent)
            finish()
        }
    }
}