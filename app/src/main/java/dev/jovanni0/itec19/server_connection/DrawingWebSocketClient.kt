package dev.jovanni0.itec19.server_connection


import android.util.Log
import dev.jovanni0.itec19.ClearEvent
import dev.jovanni0.itec19.DrawEvent
import dev.jovanni0.itec19.DrawingStore
import dev.jovanni0.itec19.HistoryEvent
import dev.jovanni0.itec19.StrokeAddedEvent
import dev.jovanni0.itec19.StrokePayload
import dev.jovanni0.itec19.UndoEvent
import dev.jovanni0.itec19.toLocalStroke
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



class DrawingWebSocketClient(
    private val posterId: String,
    private val deviceId: String,
    private val serverIp: String
) {
    private val jsonSerializer = Json {
        serializersModule = SerializersModule {
            polymorphic(DrawEvent::class) {
                subclass(StrokeAddedEvent::class)
                subclass(UndoEvent::class)
                subclass(ClearEvent::class)
                subclass(HistoryEvent::class)
            }
        }
        classDiscriminator = "type"
    }
    private val client = HttpClient(OkHttp) { install(WebSockets) }
    private var session: ClientWebSocketSession? = null


    suspend fun connect(): Boolean
    {
        try {
            client.webSocket("ws://$serverIp:8080/draw/$posterId") {
                session = this

                for (frame in incoming)
                {
                    val text = (frame as? Frame.Text)?.readText() ?: continue
                    val drawEvent = jsonSerializer.decodeFromString<DrawEvent>(text)
                    handleDrawEvent(drawEvent)
                }
            }
        }
        catch (e: Exception)
        {
            Log.d("State", "Error trying to connect to server on IP $serverIp")

            return false
        }

        return true
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
    }


    suspend fun sendUndo()
    {
        val event = UndoEvent(posterId, deviceId) as DrawEvent
        session?.send(jsonSerializer.encodeToString(event))
    }


    suspend fun sendClear()
    {
        val event = ClearEvent(posterId, deviceId) as DrawEvent
        session?.send(jsonSerializer.encodeToString(event))
    }


    /**
     * update the local store with the new data received from the server.
     */
    private fun handleDrawEvent(event: DrawEvent)
    {
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
                DrawingStore.drawings[posterId] = event.strokes.map { it.toLocalStroke() }
            }
        }
    }
}