package dev.jovanni0.itec19.server_connection

import android.util.Log
import dev.jovanni0.itec19.DominanceEvent
import dev.jovanni0.itec19.DrawEvent
import dev.jovanni0.itec19.data.Team
import dev.jovanni0.itec19.stores.DrawingStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.ClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.coroutines.cancellation.CancellationException


class DominanceWebSocketClient(
    private val serverIp: String
) {
    private val jsonSerializer = Json {
        serializersModule = SerializersModule {
            polymorphic(DrawEvent::class) {
                subclass(DominanceEvent::class)
            }
        }
        classDiscriminator = "type"
    }
    private val client = HttpClient(OkHttp) { install(WebSockets) }
    private var session: ClientWebSocketSession? = null

    val isConnected: Boolean
        get() = session != null

    suspend fun connect() {
        try {
            client.webSocket("ws://$serverIp:8080/dominance") {
                session = this
                Log.d("WebSocket", "Connected to global dominance feed")

                for (frame in incoming) {
                    val text = (frame as? Frame.Text)?.readText() ?: continue
                    val event = jsonSerializer.decodeFromString<DrawEvent>(text)
                    handleEvent(event)
                }

                session = null
                Log.d("WebSocket", "Disconnected from global dominance feed")
            }
        } catch (e: Exception) {
            session = null
            if (e is CancellationException) return
            Log.d("WebSocket", "Error connecting to global dominance feed: $e")
        }
    }

    suspend fun close() {
        session?.close()
        client.close()
    }

    private fun handleEvent(event: DrawEvent) {
        when (event) {
            is DominanceEvent -> {
                val team = event.team?.let { name -> Team.entries.firstOrNull { it.name == name } }
                DrawingStore.dominance[event.posterId] = team
                Log.d("WebSocket", "Global: ${event.team} dominates ${event.posterId}")
            }
            else -> {}
        }
    }
}