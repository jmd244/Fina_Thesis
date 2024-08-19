package com.vyw.tflite.algorithm

import android.content.res.AssetManager
import android.view.Surface


class BlazeFaceNcnn {
    external fun loadModel(mgr: AssetManager? , modelid: Int , cpugpu: Int, draw : Boolean): Boolean
    external fun openCamera(facing: Int): Boolean
    external fun closeCamera(): Boolean
    external fun setOutputWindow(surface: Surface?): Boolean
    external fun data() : FloatArray
    external fun initiateCamera(earAVG: Float, rectAVG: Float)

    companion object{
        init {
            System.loadLibrary("native-lib")
        }
    }
}