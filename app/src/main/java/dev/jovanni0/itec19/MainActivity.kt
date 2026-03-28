package dev.jovanni0.itec19

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import dev.jovanni0.itec19.ar.ArOverlayScene
import dev.jovanni0.itec19.server_connection.DrawingWebSocketClient
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var canShowAR by mutableStateOf(false)

    private val deviceId = "device1"
    private var wsClient: DrawingWebSocketClient? = null
    private var currentPosterId: String? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> canShowAR = isGranted }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        canShowAR = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

        if (!canShowAR) requestPermissionLauncher.launch(Manifest.permission.CAMERA)

        setContent {
            if (canShowAR) {
                ArOverlayScene(
                    onPosterDetected = { posterName -> switchToPoster(posterName) }
                )
            } else {
                Box(Modifier.fillMaxSize()) { Text("Waiting for Camera...") }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch { wsClient?.close() }
    }

    private fun switchToPoster(posterId: String) {
        if (currentPosterId == posterId) return
        lifecycleScope.launch {
            wsClient?.close()
            currentPosterId = posterId
            wsClient = DrawingWebSocketClient(
                posterId = posterId,
                deviceId = deviceId,
                serverIp = "10.209.127.241"
            )
            wsClient?.connect()
        }
    }
}