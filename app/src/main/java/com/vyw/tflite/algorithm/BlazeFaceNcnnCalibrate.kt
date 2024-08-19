package com.vyw.tflite.algorithm

import android.content.res.AssetManager
import android.view.Surface


class BlazeFaceNcnnCalibrate {
    var sensor : Boolean = false

    external fun loadModel(mgr: AssetManager? , modelid: Int , cpugpu: Int): Boolean
    external fun openCamera(facing: Int): Boolean
    external fun closeCamera(): Boolean
    external fun setOutputWindow(surface: Surface?): Boolean
    external fun calibrateFeature() : DoubleArray
    external fun initiateCamera()

    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}