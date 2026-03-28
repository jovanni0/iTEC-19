package dev.jovanni0.itec19

import android.graphics.Bitmap
import android.graphics.BitmapFactory
//import android.graphics.BlendMode
import androidx.compose.ui.graphics.BlendMode
//import android.graphics.Path
import androidx.compose.ui.graphics.Path
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp


class PosterDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val posterName = intent.getStringExtra("poster_name") ?: "Unknown"
        val posterBitmap = applicationContext.assets.open("$posterName.png").use {
            BitmapFactory.decodeStream(it)
        }

        setContent {
            PosterDetailScreen(
                posterName = posterName,
                posterBitmap = posterBitmap,
                onBack = { finish() }
            )
        }
    }
}

@Composable
fun PosterDetailScreen(
    posterName: String,
    posterBitmap: Bitmap,
    onBack: () -> Unit
) {
    // Drawing state
//    var paths by remember { mutableStateOf(listOf<Pair<Path, DrawConfig>>()) }
    var paths by remember {
        mutableStateOf(DrawingStore.drawings[posterName] ?: emptyList())
    }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var rawPoints by remember { mutableStateOf(listOf<Offset>()) }
    var selectedColor by remember { mutableStateOf(Color.Red) }
    var strokeWidth by remember { mutableStateOf(8f) }
    var isEraser by remember { mutableStateOf(false) }
    val bitmap = remember(posterName) { posterBitmap }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = posterName,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f).padding(vertical = 16.dp)
            )
            // Undo button
            IconButton(onClick = {
                if (paths.isNotEmpty())
                {
                    paths = paths.dropLast(1)
                    DrawingStore.drawings[posterName] = paths
                }
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "Undo")
            }
        }

        // Drawing canvas with poster background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Layer 1: Poster image (untouched)
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = posterName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.5f))
            )

            // Layer 2: Drawing canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }
                    .graphicsLayer(alpha = 0.99f)
                    .pointerInput(isEraser, selectedColor, strokeWidth) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPath = Path().apply { moveTo(offset.x, offset.y) }
                                rawPoints = listOf(offset)
                            },
                            onDrag = { change, _ ->
                                currentPath = currentPath?.apply { lineTo(change.position.x, change.position.y) }
                                rawPoints = rawPoints + change.position
                            },
//                            onDragStart = { offset ->
//                                currentPath = Path().apply { moveTo(offset.x, offset.y) }
//                            },
//                            onDrag = { change, _ ->
//                                currentPath = currentPath?.apply {
//                                    lineTo(change.position.x, change.position.y)
//                                }
//                            },
//                            onDragEnd = {
//                                currentPath?.let { path ->
//                                    paths = paths + Pair(
//                                        path,
//                                        DrawConfig(
//                                            color = if (isEraser) Color.Transparent else selectedColor,
//                                            strokeWidth = strokeWidth,
//                                            blendMode = if (isEraser) BlendMode.Clear else BlendMode.SrcOver
//                                        )
//                                    )
//                                    DrawingStore.drawings[posterName] = paths
//                                }
//                                currentPath = null
//                            }
                            onDragEnd = {
                                currentPath?.let {
                                    val normalizedPoints = rawPoints.map { it.normalize(canvasSize) }
                                    paths = paths + Pair(
                                        normalizedPoints,
                                        DrawConfig(
                                            color = if (isEraser) Color.Transparent else selectedColor,
                                            strokeWidth = strokeWidth,
                                            blendMode = if (isEraser) BlendMode.Clear else BlendMode.SrcOver
                                        )
                                    )
                                    DrawingStore.drawings[posterName] = paths
                                }
                                currentPath = null
                                rawPoints = emptyList()
                            }
                        )
                    }
            ) {
                // Draw all completed paths
//                paths.forEach { (path, config) ->
//                    drawPath(
//                        path = path,
//                        color = config.color,
//                        style = Stroke(
//                            width = config.strokeWidth,
//                            cap = StrokeCap.Round,
//                            join = StrokeJoin.Round
//                        ),
//                        blendMode = config.blendMode
//                    )
//                }
                paths.forEach { (points, config) ->
                    val path = Path().apply {
                        points.map { it.denormalize(size) }.forEachIndexed { i, offset ->
                            if (i == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = config.color,
                        style = Stroke(
                            width = config.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        ),
                        blendMode = config.blendMode
                    )
                }
                // Draw current in-progress path
                currentPath?.let { path ->
                    drawPath(
                        path = path,
                        color = if (isEraser) Color.Transparent else selectedColor,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
                        blendMode = if (isEraser) BlendMode.Clear else BlendMode.SrcOver
                    )
                }
            }
        }

        // Toolbar
        DrawingToolbar(
            selectedColor = selectedColor,
            strokeWidth = strokeWidth,
            isEraser = isEraser,
            onColorSelected = { selectedColor = it; isEraser = false },
            onStrokeWidthChanged = { strokeWidth = it },
            onEraserToggled = { isEraser = !isEraser },
            onClearAll = { paths = emptyList() }
        )
    }
}

data class DrawConfig(
    val color: Color,
    val strokeWidth: Float,
    val blendMode: BlendMode = BlendMode.SrcOver
)

@Composable
fun DrawingToolbar(
    selectedColor: Color,
    strokeWidth: Float,
    isEraser: Boolean,
    onColorSelected: (Color) -> Unit,
    onStrokeWidthChanged: (Float) -> Unit,
    onEraserToggled: () -> Unit,
    onClearAll: () -> Unit
) {
    val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Black, Color.Yellow, Color.Magenta)

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        // Color palette
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            colors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (selectedColor == color && !isEraser) 3.dp else 1.dp,
                            color = Color.Gray,
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(color) }
                )
            }
            // Eraser
            IconButton(onClick = onEraserToggled) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Eraser",
                    tint = if (isEraser) Color.Blue else Color.Gray
                )
            }
            // Clear all
            IconButton(onClick = onClearAll) {
                Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color.Red)
            }
        }

        // Stroke width slider
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Size:", modifier = Modifier.padding(end = 8.dp))
            Slider(
                value = strokeWidth,
                onValueChange = onStrokeWidthChanged,
                valueRange = 4f..40f,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

//class PosterDetailActivity : ComponentActivity()
//{
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        val posterName = intent.getStringExtra("poster_name") ?: "Unknown"
//
//        setContent {
//            Column(
//                modifier = Modifier.fillMaxSize().padding(16.dp),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                IconButton(onClick = { finish() }) {
//                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                }
//
//                Text(
//                    text = posterName,
//                    style = MaterialTheme.typography.headlineMedium,
//                    modifier = Modifier.padding(vertical = 16.dp)
//                )
//
//                // Load the poster image from assets by name
//                val bitmap = remember(posterName) {
//                    try {
//                        applicationContext.assets.open("$posterName.png").use {
//                            BitmapFactory.decodeStream(it)
//                        }
//                    } catch (e: Exception) { null }
//                }
//
//                bitmap?.let {
//                    Image(
//                        bitmap = it.asImageBitmap(),
//                        contentDescription = posterName,
//                        modifier = Modifier.fillMaxWidth(),
//                        contentScale = ContentScale.Fit
//                    )
//                }
//            }
//        }
//    }
//}