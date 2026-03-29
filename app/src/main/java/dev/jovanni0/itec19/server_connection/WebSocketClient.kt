package dev.jovanni0.itec19.server_connection


import android.util.Log
import dev.jovanni0.itec19.ClearEvent
import dev.jovanni0.itec19.DominanceEvent
import dev.jovanni0.itec19.DrawEvent
import dev.jovanni0.itec19.stores.DrawingStore
import dev.jovanni0.itec19.HistoryEvent
import dev.jovanni0.itec19.StrokeAddedEvent
import dev.jovanni0.itec19.StrokePayload
import dev.jovanni0.itec19.UndoEvent
import dev.jovanni0.itec19.data.Team
import dev.jovanni0.itec19.utils.toLocalStroke
import io.ktor.websocket.Frame
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.ClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass



class WebSocketClient(
    private val posterId: String,
    private val deviceId: String,
    private val serverIp: String,
    private val lastStrokeId: String
) {
    private val jsonSerializer = Json {
        serializersModule = SerializersModule {
            polymorphic(DrawEvent::class) {
                subclass(StrokeAddedEvent::class)
                subclass(UndoEvent::class)
                subclass(ClearEvent::class)
                subclass(HistoryEvent::class)
                subclass(DominanceEvent::class)
            }
        }
        classDiscriminator = "type"
    }
    private val client = HttpClient(OkHttp) { install(WebSockets) }
    private var session: ClientWebSocketSession? = null

    val isConnected: Boolean
        get() = session != null

    suspend fun connect()
    {
//        var disconnectedCleanly = false

        try {
//            client.webSocket("ws://$serverIp:8080/draw/$posterId") {
            client.webSocket("ws://$serverIp:8080/draw/$posterId?lastStrokeId=$lastStrokeId") {
                session = this

                Log.d("State", "Connected to server on IP $serverIp:8080")

                for (frame in incoming)
                {
                    val text = (frame as? Frame.Text)?.readText() ?: continue
                    val drawEvent = jsonSerializer.decodeFromString<DrawEvent>(text)
                    handleDrawEvent(drawEvent)
                }
//                disconnectedCleanly = true
            }
        }
        catch (e: Exception)
        {
            Log.d("State", "Error trying to connect to server on IP $serverIp:8080, ${e.toString()}")

//            if (disconnectedCleanly) return
            if (e is kotlinx.coroutines.CancellationException)
            {
                Log.d("State", "Disconnected from server on IP $serverIp:8080")
                return
            }

            Log.d("State", "Error trying to connect to server on IP $serverIp:8080, ${e.toString()}")
        }
    }


    suspend fun close()
    {
        session?.close()
        client.close()
    }


    suspend fun sendStroke(stroke: StrokePayload)
    {
        val event = StrokeAddedEvent(posterId, deviceId, stroke) as DrawEvent
        session?.send(jsonSerializer.encodeToString(event))

        Log.d("WebSocket", "Sent stroke update to server: $event")
    }


    /**
     * update the local store with the new data received from the server.
     */
    private fun handleDrawEvent(event: DrawEvent)
    {
        Log.d("WebSocket", "Got message from server: $event")

        when (event)
        {
            is StrokeAddedEvent -> {
                val current = DrawingStore.drawings[posterId] ?: emptyList()
                val newStroke = event.stroke.toLocalStroke()
                DrawingStore.drawings[posterId] = current + newStroke
            }

            is UndoEvent -> {
                val current = DrawingStore.drawings[posterId] ?: return
                // Remove last stroke from that device
                val index = current.indexOfLast { it.second.deviceId == event.deviceId }
                if (index != -1) {
                    DrawingStore.drawings[posterId] = current.toMutableList().also { it.removeAt(index) }
                }
            }

            is ClearEvent -> {
                DrawingStore.drawings[posterId] = emptyList()
            }

            is HistoryEvent -> {
                val current = DrawingStore.drawings[posterId] ?: emptyList()
                val received_strokes = event.strokes.map { it.toLocalStroke() }

                DrawingStore.drawings[posterId] = current + received_strokes

                Log.d("WebSocket", "Decoded History Event: ${DrawingStore.drawings[posterId]}")
            }

            is DominanceEvent -> {
                val dominantTeam = event.team?.let { name -> Team.entries.firstOrNull { it.name == name } }
                DrawingStore.dominance[event.posterId] = dominantTeam

                Log.d("WebSocket", "${event.team} is dominating on poster ${event.posterId}!")
            }
        }
    }
}