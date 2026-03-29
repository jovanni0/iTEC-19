package dev.jovanni0.itec19.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.jovanni0.itec19.DrawConfig
import dev.jovanni0.itec19.data.Sticker
import dev.jovanni0.itec19.stores.DrawingStore
import dev.jovanni0.itec19.stores.StickerStore
import dev.jovanni0.itec19.utils.denormalize
import dev.jovanni0.itec19.utils.savePosterAsPng
import kotlinx.coroutines.launch


class ReadOnlyPosterScreen : ComponentActivity()
{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val posterName = intent.getStringExtra("poster_name") ?: "Unknown"
        val posterBitmap = applicationContext.assets.open("$posterName.png").use {
            BitmapFactory.decodeStream(it)
        }

        setContent {
            PosterReadonlyScreen(
                posterName = posterName,
                posterBitmap = posterBitmap,
                onBack = { finish() },
            )
        }
    }
}



@Composable
fun PosterReadonlyScreen(
    posterName: String,
    posterBitmap: Bitmap,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val paths = DrawingStore.drawings[posterName] ?: emptyList()
    val stickers = StickerStore.stickers[posterName] ?: emptyList()
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    // for save-as-png
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayerRef = remember { mutableStateOf<android.graphics.Picture?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = posterName,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f).padding(vertical = 16.dp)
            )
            IconButton(onClick = {
                Log.d("State", "Save button tapped")

                coroutineScope.launch {
                    Log.d("State", "Launching save coroutine")
                    savePosterAsPng(context, posterName, posterBitmap, paths, stickers)
                }
            }) {
                Icon(Icons.Default.Share, contentDescription = "Save as PNG")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Image(
                bitmap = posterBitmap.asImageBitmap(),
                contentDescription = posterName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.5f))
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }
                    .graphicsLayer(alpha = 0.99f)
            ) {
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
            }

            // stickers
            stickers.forEach { sticker ->
                val stickerSizeDp = 80.dp
                val bmp = remember(sticker.id) {
                    val bytes = Base64.decode(sticker.data, Base64.NO_WRAP)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                bmp?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.TopStart)
                            .offset(
                                x = with(LocalDensity.current) {
                                    (sticker.position.x * canvasSize.width).toDp() - stickerSizeDp / 2
                                },
                                y = with(LocalDensity.current) {
                                    (sticker.position.y * canvasSize.height).toDp() - stickerSizeDp / 2
                                }
                            )
                            .size(stickerSizeDp)
                    )
                }
            }
        }
    }
}