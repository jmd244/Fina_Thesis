package com.vyw.tflite.maincomponent

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.vyw.tflite.CameraStarter
import com.vyw.tflite.Developer
import com.vyw.tflite.databinding.ActivityCalibrateBinding

class Calibrate: AppCompatActivity() {
    private lateinit var binding: ActivityCalibrateBinding
    private lateinit var activityBundle : Bundle
    private var thread = ThreadPool()


    private var earData = ArrayList<Double>()
    private var marData = ArrayList<Double>()
    private var rectData = ArrayList<Double>()

    private var earAverage : Double = 0.0
    private var marAverage : Double = 0.0
    private var rectAverage : Double = 0.0

    private val loadData = Runnable {
        try{
            Thread.sleep(5000)
        }catch (e : java.lang.Exception){
            e.printStackTrace()
        }

        val handler = Handler(Looper.getMainLooper())
        handler.post{
//            val intent = Intent(this, CameraStarter::class.java)
            val intent = Intent(this, Developer::class.java)
            var bundle = Bundle()
            bundle.putFloat("earAVG", earAverage.toFloat())
            bundle.putFloat("rectAVG", rectAverage.toFloat())
            bundle.putFloat("marAVG", marAverage.toFloat())
            intent.putExtra("data", bundle)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalibrateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        activityBundle = intent.getBundleExtra("earBundle")!!
        val earStringArray = activityBundle.getStringArrayList("ear")
        val marStringArray = activityBundle.getStringArrayList("mar")
        val rectStringArray = activityBundle.getStringArrayList("rect")
        if (earStringArray != null) {
            for(x in earStringArray){
                earData.add(x.toDouble())
            }
            earAverage = earData.sum() / earData.size
            earData.clear()
        }
        if(marStringArray != null){
            for (x in marStringArray){
                marData.add(x.toDouble())
            }
            marAverage = marData.sum() / marData.size
            marData.clear()
        }
        if (rectStringArray != null) {
            for (x in rectStringArray){
                rectData.add(x.toDouble())
            }
            rectAverage = rectData.sum() / rectData.size
            rectData.clear()
        }
        Log.d("EarData", "Data : $earData \nAverage : $earAverage")

        thread.execute(loadData)
    }
}