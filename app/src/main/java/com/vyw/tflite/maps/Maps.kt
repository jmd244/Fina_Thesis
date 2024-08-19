package com.vyw.tflite.maps

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.vyw.tflite.R
import com.vyw.tflite.databinding.ActivityMapsBinding

class Maps : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var map: GoogleMap
    private lateinit var locationManager : LocationManager
    private lateinit var binder: ActivityMapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binder = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binder.root)
        locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binder.myLoc.setOnClickListener{
            val dialogBinding = layoutInflater.inflate(R.layout.dialog_alert2, null)
            val dialog = Dialog(this)
            dialog.setContentView(dialogBinding)

            dialog.window?.setDimAmount(0F)
            dialog.window?.setWindowAnimations(R.style.CustomDialogTheme)
            dialog.setCancelable(false)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            dialog.show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Set the map type to satellite
        map.mapType = GoogleMap.MAP_TYPE_SATELLITE

        // Add a marker at a specific location
        val myLocation = getLocation()
        var coords = myLocation?.let { LatLng(it.latitude, it.longitude) }
        coords?.let { MarkerOptions().position(it).title("Your location") }?.let { map.addMarker(it) }

        // Move the camera to the marker
        coords?.let { CameraUpdateFactory.newLatLngZoom(it, 16f) }?.let { map.moveCamera(it) }
    }

    @SuppressLint("MissingPermission")
    fun getLocation(): Location? {
        var location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (location == null) {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }
        return location
    }
}
