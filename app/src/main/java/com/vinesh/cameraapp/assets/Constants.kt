package com.vinesh.cameraapp.assets

import android.Manifest
import androidx.camera.core.CameraSelector

object Constants {

    const val FILE_NAME_FORMAT = "yy-MM-dd-HH-mm-ss-SSS"
    const val REQUEST_CODE_PERMISSIONS = 1205
    val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    const val ENCRYPTED_SHARED_PREF_NAME = "encrypted_pref"
    var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

}