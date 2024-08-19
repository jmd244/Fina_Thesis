package com.vyw.tflite.onboarding

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.vyw.tflite.R
import com.vyw.tflite.permission.camera_placement

class OnboardingActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val nextButton = findViewById<Button>(R.id.nextButton_1)
        nextButton.setOnClickListener {
            val intent = Intent(this, camera_placement::class.java)
            startActivity(intent)
            finish()
        }
    }
}


