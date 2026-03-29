package dev.jovanni0.itec19.server_connection

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


object DominanceWebSocketManager
{
    private var client: DominanceWebSocketClient? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    fun connect(serverIp: String)
    {
        Log.d("WebSocket", "Connecting to room dominance room")

        scope.launch {
            client?.close()
            client = DominanceWebSocketClient(serverIp)
            client?.connect()
        }
    }


    fun close()
    {
        scope.launch {
            client?.close()
            client = null

            Log.d("WebSocket", "Disconnected from dominance room")
        }
    }
}