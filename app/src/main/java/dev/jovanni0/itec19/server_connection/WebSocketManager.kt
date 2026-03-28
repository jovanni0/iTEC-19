package dev.jovanni0.itec19.server_connection

import android.util.Log
import dev.jovanni0.itec19.StrokePayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch



object WebSocketManager
{
    private var client: DrawingWebSocketClient? = null
    private var currentPosterId: String? = null


    fun connect(posterId: String, deviceId: String, serverIp: String, scope: CoroutineScope)
    {
        if (currentPosterId == posterId) return

        scope.launch {
            client?.close()
            currentPosterId = posterId
            client = DrawingWebSocketClient(posterId, deviceId, serverIp)
            client?.connect()

            Log.d("State", "WebSocketManager connected to room $currentPosterId")
        }
    }


    fun sendStroke(stroke: StrokePayload, scope: CoroutineScope)
    {
        scope.launch {
            client?.sendStroke(stroke)
        }

        Log.d("State", "Sent stroke to server in room $currentPosterId")
    }


    fun close(scope: CoroutineScope)
    {
        scope.launch {
            client?.close()

            Log.d("State", "WebSocketManager disconnected from room $currentPosterId")

            client = null
            currentPosterId = null
        }
    }
}