package dev.jovanni0.itec19.utils

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import com.google.ar.core.Config
import com.google.ar.core.Session


//fun toggleFlashlight(context: Context, enabled: Boolean)
//{
//    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
//    val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
//        cameraManager.getCameraCharacteristics(id)
//            .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
//    } ?: return
//    cameraManager.setTorchMode(cameraId, enabled)
//}

fun toggleFlashlight(context: Context, session: Session?, enabled: Boolean) {
    try {
        session?.pause()
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraManager.setTorchMode("0", enabled)
        session?.resume()
    } catch (e: Exception) {
        Log.e("Flashlight", "Failed to toggle: $e")
    }
}