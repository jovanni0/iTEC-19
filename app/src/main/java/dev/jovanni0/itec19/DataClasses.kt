package dev.jovanni0.itec19

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import dev.jovanni0.itec19.data.Team
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator


@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class DrawEvent {
    abstract val posterId: String
    abstract val deviceId: String
}


@Serializable
@SerialName("StrokeAddedEvent")
data class StrokeAddedEvent(
    override val posterId: String,
    override val deviceId: String,
    val stroke: StrokePayload
) : DrawEvent()


@Serializable
@SerialName("UndoEvent")
data class UndoEvent(
    override val posterId: String,
    override val deviceId: String
) : DrawEvent()


@Serializable
@SerialName("ClearEvent")
data class ClearEvent(
    override val posterId: String,
    override val deviceId: String
) : DrawEvent()


@Serializable
@SerialName("HistoryEvent")
data class HistoryEvent(          // server → late joiner on connect
    override val posterId: String,
    override val deviceId: String = "server",
    val strokes: List<StrokePayload>
) : DrawEvent()


@Serializable
data class StrokePayload(
    val id: String,
    val deviceId: String,
    val points: List<NormalizedOffset>,
    val config: DrawConfigPayload
)


@Serializable
data class NormalizedOffset(val x: Float, val y: Float)


@Serializable
data class DrawConfigPayload(
    val color: Int,
    val strokeWidth: Float,
    val isEraser: Boolean,
    val team: Team
)


data class DrawConfig(
    val color: Color,
    val strokeWidth: Float,
    val blendMode: BlendMode = BlendMode.SrcOver,
    val deviceId: String,
    val strokeId: String
)


@Serializable
@SerialName("DominanceEvent")
data class DominanceEvent(
    override val posterId: String,
    override val deviceId: String = "server",
    val team: String?
) : DrawEvent()