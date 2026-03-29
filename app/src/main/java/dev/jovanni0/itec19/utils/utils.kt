package dev.jovanni0.itec19.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import dev.jovanni0.itec19.DrawConfig
import dev.jovanni0.itec19.StrokePayload


fun Offset.normalize(canvasSize: Size): Offset
{
    return Offset(x / canvasSize.width, y / canvasSize.height)
}


fun Offset.denormalize(canvasSize: Size): Offset
{
    return Offset(x * canvasSize.width, y * canvasSize.height)
}


fun StrokePayload.toLocalStroke(): Pair<List<Offset>, DrawConfig>
{
    return Pair(
        points.map { Offset(it.x, it.y) },
        DrawConfig(
            color = Color(config.color),
            strokeWidth = config.strokeWidth,
            blendMode = if (config.isEraser) BlendMode.Clear else BlendMode.SrcOver,
            deviceId = deviceId,
            strokeId = id
        )
    )
}