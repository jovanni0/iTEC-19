package dev.jovanni0.itec19.screen

import android.R
import android.content.Intent
import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.opengl.Matrix
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
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
import dev.jovanni0.itec19.server_connection.WebSocketManager
import dev.jovanni0.itec19.stores.AppStore
import dev.jovanni0.itec19.stores.DrawingStore
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

                    // Connect only if this is a new poster
//                    if (connectedPosterId != visiblePoster.name)
//                    {
//                        Log.d("WebSocket", "Triggered connection for new poster ${visiblePoster.name}")
//
////                        WebSocketManager.close()
//
//                        WebSocketManager.connect(
//                            visiblePoster.name,
//                            AppStore.deviceId,
//                            AppStore.SERVER_IP,
//                            DrawingStore.getLastStrokeId(visiblePoster.name).toString(),
//                            scope
//                        )
//                        connectedPosterId = visiblePoster.name
//                    }

                    if (connectedPosterId != visiblePoster.name)
                    {
                        Log.d("WebSocket", "Triggered connection for new poster ${visiblePoster.name}")
                        val targetPoster = visiblePoster.name
                        connectedPosterId = targetPoster

                        scope.launch {
                            WebSocketManager.close()
                            WebSocketManager.connect(
                                targetPoster,
                                AppStore.deviceId,
                                AppStore.SERVER_IP,
                                DrawingStore.getLastStrokeId(targetPoster).toString(),
                                scope
                            )
                        }
                    }

                    statusMessage = "Found ${visiblePoster.name}"

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

        if (cornerPoints.size == 4) {
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

                    if (!posterDrawings.isNullOrEmpty()) {
                        val quadOrigin = cornerPoints[0]
                        val quadRight = cornerPoints[1] - cornerPoints[0]
                        val quadDown = cornerPoints[3] - cornerPoints[0]

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