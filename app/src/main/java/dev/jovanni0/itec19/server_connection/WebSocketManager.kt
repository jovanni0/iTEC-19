package dev.jovanni0.itec19.server_connection

import android.util.Log
import dev.jovanni0.itec19.StrokePayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


object WebSocketManager
{
    private var client: WebSocketClient? = null
    private var currentPosterId: String? = null


    fun connect(
        posterId: String,
        deviceId: String,
        serverIp: String,
        lastStrokeId: String,
        scope: CoroutineScope,
    ) {
        if (currentPosterId == posterId) return

        scope.launch {
            client?.close()
            currentPosterId = posterId
            client = WebSocketClient(
                posterId,
                deviceId,
                serverIp,
                lastStrokeId = lastStrokeId,
            )
            client?.connect()
        }
        Log.d("State", "WebSocketManager tried connecting to room $currentPosterId")
    }


    fun sendStroke(stroke: StrokePayload, scope: CoroutineScope)
    {
        if (client?.isConnected == null)
        {
            Log.d("WebSocket", "Could not send stroke because not connected to server.")
            return
        }

        scope.launch {
            client?.sendStroke(stroke)
        }

        Log.d("State", "Sent stroke to server in room $currentPosterId")
    }

    fun close()
    {
        runBlocking {
            client?.close()
        }

        Log.d("State", "WebSocketManager disconnected from room $currentPosterId")

        client = null
        currentPosterId = null
    }
}