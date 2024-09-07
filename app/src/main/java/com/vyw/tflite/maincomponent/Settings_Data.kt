package com.vyw.tflite.maincomponent

import android.content.Context
import android.content.SharedPreferences

data class SettingsData(
    var ringtone : Int = 0,
    var fps : Int = 0,
    var volume: Int = 0,
    var contact : String = ""
)

data class Calibration(
    var earAVG : Float = 0f,
    var headPoseAVG : Float = 0f,
    var rectAVG : Float = 0.0f
)

class SettingsPref(context: Context) {
    private var myContext : Context = context
    private var preferences : SharedPreferences

    init {
        preferences = myContext.getSharedPreferences("Settings", Context.MODE_PRIVATE)
    }

    fun getSettings() : SettingsData? {
        val contactNum = preferences.getString("contact", "")

        return contactNum?.let {
            SettingsData(
                fps = preferences.getInt("fps", 15),
                ringtone = preferences.getInt("ringtone", 0),
                volume = preferences.getInt("volume", 50),
                contact = it
            )
        }
    }

    fun getContactNumber() : String? {
        return preferences.getString("contact", "")
    }

    fun getVolume () : Int {
        return preferences.getInt("volume", 50)
    }

    fun saveContactNumber(contact : String) : String {
        val editor = preferences.edit()
        editor.putString("contact", contact)
        editor.apply()
        return contact
    }

    fun saveVolume(volume: Int) : Int {
        val editor = preferences.edit()
        editor.putInt("volume", volume)
        editor.apply()
        return volume
    }

    fun saveSoundSpinner(sound: Int) : Int {
        val editor = preferences.edit()
        editor.putInt("sound", sound)
        editor.apply()
        return sound
    }

    fun getPreference() : SharedPreferences{
        return preferences
    }
}
