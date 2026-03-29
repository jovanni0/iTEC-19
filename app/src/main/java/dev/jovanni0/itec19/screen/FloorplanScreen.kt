package dev.jovanni0.itec19.screen

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jovanni0.itec19.data.PosterMarker
import dev.jovanni0.itec19.stores.DrawingStore


val posterMarkers = listOf(
//    PosterMarker("afis1", 0.31f, 0.72f),
//    PosterMarker("afis2", 0.83f, 0.9f),
//    PosterMarker("afis3", 0.58f, 0.97f),
//    PosterMarker("afis4", 0.55f, 0.8f),
//    PosterMarker("afis5", 0.32f, 0.75f),
//    PosterMarker("afis6", 0.1f, 0.5f),
//    PosterMarker("afis7", 0.8f, 0.7f),
//    PosterMarker("afis8", 0.4f, 0.1f),
//    PosterMarker("afis9", 0.9f, 0.3f),
//    PosterMarker("afis10", 0.5f, 0.9f),
    PosterMarker("afis1",  0.31f, 0.58f),
    PosterMarker("afis2",  0.82f, 0.67f),
    PosterMarker("afis3",  0.60f, 0.70f),
    PosterMarker("afis4",  0.56f, 0.62f),
    PosterMarker("afis5",  0.31f, 0.62f),
    PosterMarker("afis6",  0.72f, 0.62f),
    PosterMarker("afis7",  0.15f, 0.49f),
    PosterMarker("afis8",  0.82f, 0.59f),
    PosterMarker("afis9",  0.66f, 0.62f),
    PosterMarker("afis10", 0.60f, 0.64f),
)


@Composable
fun FloorplanScreen(modifier: Modifier = Modifier)
{
    val context = LocalContext.current
    val mapBitmap = remember {
        context.assets.open("floorplan.png").use { BitmapFactory.decodeStream(it) }
    }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset += panChange
    }

    Box(
        modifier = modifier
            .transformable(state = transformState)
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = 1f
                        offset = Offset.Zero
                    }
                )
            }
    ) {
        val imageAspect = mapBitmap.width.toFloat() / mapBitmap.height
        val boxAspect = if (imageSize != IntSize.Zero) imageSize.width.toFloat() / imageSize.height else imageAspect

        val (renderedW, renderedH, offsetX, offsetY) = if (imageSize != IntSize.Zero) {
            if (imageAspect > boxAspect) {
                val w = imageSize.width.toFloat()
                val h = w / imageAspect
                listOf(w, h, 0f, (imageSize.height - h) / 2f)
            } else {
                val h = imageSize.height.toFloat()
                val w = h * imageAspect
                listOf(w, h, (imageSize.width - w) / 2f, 0f)
            }
        } else listOf(0f, 0f, 0f, 0f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        ) {
            Image(
                bitmap = mapBitmap.asImageBitmap(),
                contentDescription = "Floor plan",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { imageSize = it }
            )

            if (imageSize != IntSize.Zero)
            {
                posterMarkers.forEach { marker ->
                    val dominantTeam = DrawingStore.dominance[marker.posterId]
                    val markerColor = dominantTeam?.color ?: Color.White

                    val x = offsetX + marker.x * renderedW
                    val y = offsetY + marker.y * renderedH

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(x.toInt() - 20, y.toInt() - 20) }
                            .size(10.dp)
                            .background(markerColor.copy(alpha = 0.8f), CircleShape)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = marker.posterId,
                            color = Color.White,
                            fontSize = 8.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        posterMarkers.forEach { marker ->
            val centerX = imageSize.width / 2f
            val centerY = imageSize.height / 2f

            val rawX = offsetX + marker.x * renderedW
            val rawY = offsetY + marker.y * renderedH

            val x = (rawX - centerX) * scale + centerX + offset.x
            val y = (rawY - centerY) * scale + centerY + offset.y

            Text(
                text = marker.posterId,
                color = Color.Black,
                fontSize = 18.sp,
                modifier = Modifier
                    .offset { IntOffset(x.toInt() - 20, y.toInt() + 24) }
                    .padding(4.dp)
                    .background(Color.White)
            )
        }
    }
}