package dev.jovanni0.itec19

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size


fun Offset.normalize(canvasSize: Size): Offset
{
    return Offset(x / canvasSize.width, y / canvasSize.height)
}


fun Offset.denormalize(canvasSize: Size): Offset
{
    return Offset(x * canvasSize.width, y * canvasSize.height)
}