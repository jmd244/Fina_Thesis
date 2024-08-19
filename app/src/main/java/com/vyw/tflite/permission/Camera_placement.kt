package com.vyw.tflite.permission

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.vyw.tflite.MainActivity
import com.vyw.tflite.R
import com.vyw.tflite.onboarding.OnboardingActivity

class camera_placement : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_placement)

        val nextBtn = findViewById<Button>(R.id.nextButton)
        nextBtn.setOnClickListener {
            val intent = Intent(this, Permission_Location::class.java)
            startActivity(intent)
            finish()
        }

        val backBtn = findViewById<Button>(R.id.backButton)
        backBtn.setOnClickListener {
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}