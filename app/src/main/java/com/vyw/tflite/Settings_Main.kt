package com.vyw.tflite

import android.R.raw
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.vyw.tflite.databinding.ActivitySettingsBinding
import com.vyw.tflite.maincomponent.SettingsPref
import java.lang.reflect.Field
import com.vyw.tflite.maps.Maps

class Settings_Main : AppCompatActivity() {
    private lateinit var settings : SettingsPref
    private lateinit var binder: ActivitySettingsBinding
    private val locationPermissionCode = 2
    var media = MediaPlayer()

    private val PERMISSIONS_REQUEST_READ_CONTACTS = 100
    private val PICK_CONTACT_REQUEST = 101

    var fields: Array<Field> = raw::class.java.fields

    var contactNum = ""
    var volume = 50
    var sound = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binder = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binder.root)

        settings = SettingsPref(this)
        contactNum = settings.getPreference().getString("contact", "")!!
        volume = settings.getPreference().getInt("volume", 50)
        sound = settings.getPreference().getInt("sound", 0)

        binder.mobileNum.setText(contactNum)
        binder.volumeSlider.progress = volume
        binder.volumePercentage.text = "$volume%"
        binder.soundSpinner.setSelection(sound)

        try{
            val items = ArrayList<String>()
            val ringtoneAsset = resources.assets.list("Ringtones/")

            for(i in 1..(ringtoneAsset?.size ?: 0)){
                items.add("Set $i")
            }
            val adapter : ArrayAdapter<String> = ArrayAdapter<String>(this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item , items)
            binder.soundSpinner.adapter = adapter
        }catch (e : Exception){
            e.printStackTrace()
        }

        val returnIcon = findViewById<ImageView>(R.id.back)
        returnIcon.setOnClickListener {
            startActivity(Intent(this@Settings_Main, MainActivity::class.java))
        }

        val mobileNum = binder.mobileNum
        var mobileOriginalText = mobileNum.text.toString()
        var isNumber = false
        var isRequirementSatisfied = false
        mobileNum.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                mobileOriginalText = p0.toString()
                isNumber = mobileOriginalText.matches(Regex("^[0-9]*$"))
                isRequirementSatisfied = mobileOriginalText.length >= 11
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                Log.d("Changed", p0.toString())
                if(!isRequirementSatisfied || p0.toString().length < mobileOriginalText.length && isNumber){
                    Log.d("Changed", p0.toString())
                }else{
                    mobileNum.removeTextChangedListener(this)
                    mobileNum.setText(mobileOriginalText)
                    mobileNum.addTextChangedListener(this)
                }
            }

            override fun afterTextChanged(p0: Editable?) {}

        })

        for(item in fields){
            Log.v(
                "RawField",
                "$item"
            )
        }

        val btnaddContact = binder.btnaddContact
        btnaddContact.setOnClickListener{
            val number = mobileNum.text.toString()
            val regex = Regex("^09\\d+")
            val isPatternCorrect = regex.matches(mobileNum.text)
            if(isPatternCorrect && number.length == 11){
                contactNum = settings.saveContactNumber(number)
            }else{
                Toast.makeText(this, "Invalid mobile number", Toast.LENGTH_SHORT).show()
            }
        }

        val soundSpinner = binder.soundSpinner
        soundSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                // Get the selected item
                sound = settings.saveSoundSpinner(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                // This method is called when the selection is cleared
            }
        }

        //SMS button
        val smsBtn = binder.smsbtn
        smsBtn.setOnClickListener {
            val phoneNumber = binder.mobileNum.text.toString()
            val message = "Sample message"
            val smsManager = SmsManager.getDefault() as SmsManager
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
        }

        val volumeSlidder = binder.volumeSlider
        volumeSlidder.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // Update the UI or perform some action when the progress changes
                volume = settings.saveVolume(progress)
                binder.volumePercentage.text = "$progress%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Called when the user starts interacting with the SeekBar
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Perform some action when the user releases the SeekBar
            }
        })

        //Test alert button
        val btnTestAlert = binder.testAlert
        btnTestAlert.setOnClickListener {
            if(media.isPlaying){
                media.stop()
                media.release()
                media = MediaPlayer()
                binder.testAlert.text = "TEST ALERT"
            }else{
                var ringtoneIndx = binder.soundSpinner.selectedItemPosition + 1
                val path = "Ringtones/set$ringtoneIndx"
                var fd =resources.assets.list(path)
                if (fd != null) {
                    for(file in fd){
                        Log.v("MyAssets", "$file")
                    }
                }

                try{
                    if(media.isPlaying){
                        media.stop()
                        media.release()
                        media = MediaPlayer()
                    }
                    val descriptor : AssetFileDescriptor = assets.openFd("$path/a${binder.soundSpinner.selectedItemPosition+1}.mp3")
                    media.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
                    descriptor.close()

                    media.prepare()
                    media.setVolume(volume.toFloat()/100, volume.toFloat()/100)
                    media.start()
                    binder.testAlert.text = "STOP"
                }catch (e : Exception){
                    e.printStackTrace()
                }
                media.setOnCompletionListener {
                    binder.testAlert.text = "TEST ALERT"
                }
            }
        }

        val btnLoc = binder.locButton
        btnLoc.setOnClickListener{
            val intent = Intent(this, Maps::class.java)
            startActivity(intent)
        }
    }
    private fun pickContact() {
        val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK) {
            val contactUri = data?.data
            val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val cursor = contentResolver.query(contactUri!!, projection, null, null, null)

            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex("data1")

                if (columnIndex != -1) {
                    val phoneNumber = cursor.getString(columnIndex)
//                    binder.mobileNum.text = phoneNumber
                } else {
                    Toast.makeText(
                        this,
                        "Unable to retrieve contact phone number",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                cursor.close()
            } else {
                Toast.makeText(
                    this,
                    "Unable to retrieve contact information",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun locClick(view: View) {}
}