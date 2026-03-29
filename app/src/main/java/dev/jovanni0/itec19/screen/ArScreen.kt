package dev.jovanni0.itec19.screen

import android.R
import android.content.Intent
import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.opengl.Matrix
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.google.ar.core.AugmentedImage
import com.google.ar.core.Config
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import dev.jovanni0.itec19.PosterDetailActivity
import dev.jovanni0.itec19.ar.buildImageDatabase
import dev.jovanni0.itec19.ar.isPointInQuad
import dev.jovanni0.itec19.ar.mapNormalizedStrokeToQuad
import dev.jovanni0.itec19.audio.TeamAudioPlayer
import dev.jovanni0.itec19.server_connection.WebSocketManager
import dev.jovanni0.itec19.stores.AppStore
import dev.jovanni0.itec19.stores.DrawingStore
import dev.jovanni0.itec19.stores.StickerStore
import io.github.sceneview.ar.ARScene
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.collections.get

@Composable
fun ArScreen(assets: AssetManager, modifier: Modifier = Modifier, isActive: Boolean = true)
{
    var statusMessage by remember { mutableStateOf("Scan a poster") }
    var trackedImage by remember { mutableStateOf<AugmentedImage?>(null) }
    var cornerPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val context = LocalContext.current


    // server connection
    var connectedPosterId by remember { mutableStateOf<String?>(null) }
    var disconnectJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    DisposableEffect(isActive) {
        onDispose { TeamAudioPlayer.stop() }
    }


    // haptic feedback
    val haptic = LocalHapticFeedback.current
    val currentDrawings = DrawingStore.drawings[trackedImage?.name]

    LaunchedEffect(currentDrawings?.size) {
        val lastStroke = currentDrawings?.lastOrNull() ?: return@LaunchedEffect
        val isOtherTeam = lastStroke.second.deviceId != AppStore.deviceId

        if (isOtherTeam) {
//            repeat(3) {
//                delay(100)
//            }
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }


    fun configureSession(session: Session) {
        val config = Config(session).apply {
            augmentedImageDatabase = buildImageDatabase(session, assets)
            focusMode = Config.FocusMode.AUTO
            updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        }
        session.configure(config)
    }

    Box(modifier = modifier)
    {
        ARScene(
            modifier = Modifier.fillMaxSize(),
            onSessionCreated = { session -> configureSession(session) },
            onSessionUpdated = { _, frame ->
                val visiblePoster = frame.getUpdatedTrackables(AugmentedImage::class.java)
                    .firstOrNull {
                        it.trackingState == TrackingState.TRACKING &&
                                it.trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING
                    }
                trackedImage = visiblePoster

                if (visiblePoster != null)
                {
                    disconnectJob?.cancel()
                    disconnectJob = null

                    if (connectedPosterId != visiblePoster.name)
                    {
                        Log.d("WebSocket", "Triggered connection for new poster ${visiblePoster.name}")
                        val targetPoster = visiblePoster.name
                        connectedPosterId = targetPoster

                        WebSocketManager.connect(
                            targetPoster,
                            AppStore.deviceId,
                            AppStore.SERVER_IP,
                            DrawingStore.getLastStrokeId(targetPoster).toString(),
                        )
                    }

                    statusMessage = "Found ${visiblePoster.name}"

                    val dominantTeam = DrawingStore.dominance[visiblePoster.name]
                    if (dominantTeam != null && dominantTeam != AppStore.team) {
                        TeamAudioPlayer.play(context, dominantTeam)
                    } else {
                        TeamAudioPlayer.stop()
                    }

                    try {
                        val camera = frame.camera
                        val projectionMatrix = FloatArray(16)
                        val viewMatrix = FloatArray(16)
                        camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)
                        camera.getViewMatrix(viewMatrix, 0)

                        val halfW = visiblePoster.extentX / 2f
                        val halfH = visiblePoster.extentZ / 2f

                        val corners = listOf(
                            visiblePoster.centerPose.compose(Pose.makeTranslation(-halfW, 0f, -halfH)),
                            visiblePoster.centerPose.compose(Pose.makeTranslation( halfW, 0f, -halfH)),
                            visiblePoster.centerPose.compose(Pose.makeTranslation( halfW, 0f,  halfH)),
                            visiblePoster.centerPose.compose(Pose.makeTranslation(-halfW, 0f,  halfH))
                        )

                        cornerPoints = corners.mapNotNull { pose ->
                            val worldPos = floatArrayOf(pose.tx(), pose.ty(), pose.tz(), 1f)

                            val viewPos = FloatArray(4)
                            Matrix.multiplyMV(viewPos, 0, viewMatrix, 0, worldPos, 0)

                            val clipPos = FloatArray(4)
                            Matrix.multiplyMV(clipPos, 0, projectionMatrix, 0, viewPos, 0)

                            if (clipPos[3] == 0f) return@mapNotNull null

                            val ndcX = clipPos[0] / clipPos[3]
                            val ndcY = clipPos[1] / clipPos[3]

                            Offset(
                                x = (ndcX + 1f) / 2f * canvasSize.width,
                                y = (1f - ndcY) / 2f * canvasSize.height
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("AR_OVERLAY", "Corner projection failed", e)
                    }
                } else {
                    statusMessage = "Looking for poster..."
                    cornerPoints = emptyList()

                    TeamAudioPlayer.stop()

                    if (disconnectJob == null && connectedPosterId != null)
                    {
                        disconnectJob = scope.launch {
                            delay(3000)
                            WebSocketManager.close()
                            connectedPosterId = null
                            disconnectJob = null

                            Log.d("WebSocket", "Triggered disconnection from server because poster left frame")
                        }
                    }
                }
            }
        )

        Box(modifier = Modifier.fillMaxSize().onSizeChanged { canvasSize = it })

        if (cornerPoints.size == 4)
        {
            /**
             * stickers
             */
            val posterStickers = StickerStore.stickers[trackedImage?.name]
            val decodedStickers = posterStickers?.map { sticker ->
                sticker to remember(sticker.id) {
                    val bytes = android.util.Base64.decode(sticker.data, android.util.Base64.NO_WRAP)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            }


            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isActive) Modifier.pointerInput(Unit)
                        {
                            detectTapGestures { tapOffset ->
                                Log.d("AR_TAP", "Tapped at $tapOffset")
                                if (cornerPoints.size == 4 && isPointInQuad(tapOffset, cornerPoints)) {
                                    val posterName = trackedImage?.name ?: return@detectTapGestures
                                    val intent = Intent(context, PosterDetailActivity::class.java).apply {
                                        putExtra("poster_name", posterName)
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        }
                        else Modifier
                    )
            ) {
                if (cornerPoints.size == 4) {
                    val path = Path().apply {
                        moveTo(cornerPoints[0].x, cornerPoints[0].y)
                        lineTo(cornerPoints[1].x, cornerPoints[1].y)
                        lineTo(cornerPoints[2].x, cornerPoints[2].y)
                        lineTo(cornerPoints[3].x, cornerPoints[3].y)
                        close()
                    }
//                    drawPath(path, color = Color(0x440000FF))
//                    drawPath(path, color = Color(0xFF00E5FF), style = Stroke(width = 4f))
                    drawPath(
                        path,
//                        color = AppStore.team?.color ?: Color.White,
                        color = DrawingStore.dominance[trackedImage?.name]?.color ?: Color.White,
                        style = Stroke(width = 8f)
                    )

                    val posterName = trackedImage?.name
                    val posterDrawings = DrawingStore.drawings[posterName]
                    Log.d("AR_DRAW", "Poster: $posterName, Drawings: ${posterDrawings?.size}")
                    Log.d("AR_DRAW", "Corner points: $cornerPoints")

                    val quadOrigin = cornerPoints[0]
                    val quadRight = cornerPoints[1] - cornerPoints[0]
                    val quadDown = cornerPoints[3] - cornerPoints[0]

                    if (!posterDrawings.isNullOrEmpty())
                    {
                        Log.d("AR_DRAW", "quadOrigin=$quadOrigin, quadRight=$quadRight, quadDown=$quadDown")

                        posterDrawings.forEach { (points, config) ->
                            val mappedPath = mapNormalizedStrokeToQuad(points, quadOrigin, quadRight, quadDown)
                            drawPath(
                                path = mappedPath,
                                color = config.color,
                                style = Stroke(width = config.strokeWidth)
                            )
                        }
                    }

                    decodedStickers?.forEach { (sticker, bmp) ->
                        bmp?.let {
                            val mapped = quadOrigin + quadRight * sticker.position.x + quadDown * sticker.position.y
                            val stickerSize = quadRight.getDistance() * 0.2f

                            drawImage(
                                image = it.asImageBitmap(),
                                dstOffset = androidx.compose.ui.unit.IntOffset(
                                    (mapped.x - stickerSize / 2).toInt(),
                                    (mapped.y - stickerSize / 2).toInt()
                                ),
                                dstSize = androidx.compose.ui.unit.IntSize(
                                    stickerSize.toInt(),
                                    stickerSize.toInt()
                                )
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = statusMessage,
            color = AppStore.team?.color ?: Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(40.dp),
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}