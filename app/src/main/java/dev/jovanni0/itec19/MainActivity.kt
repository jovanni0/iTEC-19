package dev.jovanni0.itec19

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.sceneview.ar.ARScene
import com.google.ar.core.Config
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.ar.core.AugmentedImage
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import android.opengl.Matrix
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.google.ar.core.Pose
import kotlin.jvm.java

class MainActivity : ComponentActivity()
{
    private var statusMessage by mutableStateOf("Scan a poster")
    private var canShowAR by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        canShowAR = isGranted
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        // Check if permission is already there
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
        {
            canShowAR = true
        }
        else
        {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            if (canShowAR) {
                AppContent(statusMessage)
            } else {
                Box(Modifier.fillMaxSize()) { Text("Waiting for Camera...") }
            }
        }
    }

    private fun configureSession(session: Session)
    {
        val config = Config(session).apply {
            augmentedImageDatabase = buildImageDatabase(session)
            focusMode = Config.FocusMode.AUTO
            updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        }
        session.configure(config)
    }

    @Composable
    fun AppContent(message: String) {
        var trackedImage by remember { mutableStateOf<AugmentedImage?>(null) }
        var cornerPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
        var canvasSize by remember { mutableStateOf(IntSize.Zero) }
        val context = LocalContext.current

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
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

                    if (visiblePoster != null) {
                        statusMessage = "Found ${visiblePoster.name}"

                        try {
                            val camera = frame.camera
                            val projectionMatrix = FloatArray(16)
                            val viewMatrix = FloatArray(16)
                            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)
                            camera.getViewMatrix(viewMatrix, 0)

                            // Half-dimensions of the poster in meters
                            val halfW = visiblePoster.extentX / 2f
                            val halfH = visiblePoster.extentZ / 2f

                            // Translate the center pose to each corner
                            val corners = listOf(
                                visiblePoster.centerPose.compose(Pose.makeTranslation(-halfW, 0f, -halfH)), // TOP_LEFT
                                visiblePoster.centerPose.compose(Pose.makeTranslation( halfW, 0f, -halfH)), // TOP_RIGHT
                                visiblePoster.centerPose.compose(Pose.makeTranslation( halfW, 0f,  halfH)), // BOTTOM_RIGHT
                                visiblePoster.centerPose.compose(Pose.makeTranslation(-halfW, 0f,  halfH))  // BOTTOM_LEFT
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
                            android.util.Log.e("AR_OVERLAY", "Corner projection failed", e)
                        }
                    } else {
                        statusMessage = "Looking for poster..."
                        cornerPoints = emptyList()
                    }
                }
            )

            Box(modifier = Modifier.fillMaxSize().onSizeChanged { canvasSize = it })

            // Draw the quad overlay over the poster corners
            if (cornerPoints.size == 4) {
//                Canvas(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .pointerInput(cornerPoints) {
//                            detectTapGestures { tapOffset ->
//                                android.util.Log.d("AR_TAP", "Tapped at $tapOffset")
//                                android.util.Log.d("AR_TAP", "Corner points: $cornerPoints")
//
//                                if (cornerPoints.size == 4) {
//                                    val inside = isPointInQuad(tapOffset, cornerPoints)
//                                    android.util.Log.d("AR_TAP", "Inside quad: $inside")
//                                    if (inside) {
//                                        val posterName = trackedImage?.name ?: return@detectTapGestures
//                                        val intent = Intent(context, PosterDetailActivity::class.java).apply {
//                                            putExtra("poster_name", posterName)
//                                        }
//                                        context.startActivity(intent)
//                                    }
//                                } else {
//                                    android.util.Log.d("AR_TAP", "Not enough corner points: ${cornerPoints.size}")
//                                }
//                            }
//                        }
//                ) {
//                    val path = Path().apply {
//                        moveTo(cornerPoints[0].x, cornerPoints[0].y)
//                        lineTo(cornerPoints[1].x, cornerPoints[1].y)
//                        lineTo(cornerPoints[2].x, cornerPoints[2].y)
//                        lineTo(cornerPoints[3].x, cornerPoints[3].y)
//                        close()
//                    }
//                    drawPath(path, color = Color(0x440000FF))          // semi-transparent fill
//                    drawPath(path, color = Color(0xFF00E5FF), style = Stroke(width = 4f))  // border
//                }
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { tapOffset ->
                                android.util.Log.d("AR_TAP", "Tapped at $tapOffset")
                                if (cornerPoints.size == 4 && isPointInQuad(tapOffset, cornerPoints)) {
                                    val posterName = trackedImage?.name ?: return@detectTapGestures
                                    val intent = Intent(context, PosterDetailActivity::class.java).apply {
                                        putExtra("poster_name", posterName)
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        }
                ) {
                    if (cornerPoints.size == 4) {
                        val path = Path().apply {
                            moveTo(cornerPoints[0].x, cornerPoints[0].y)
                            lineTo(cornerPoints[1].x, cornerPoints[1].y)
                            lineTo(cornerPoints[2].x, cornerPoints[2].y)
                            lineTo(cornerPoints[3].x, cornerPoints[3].y)
                            close()
                        }
                        drawPath(path, color = Color(0x440000FF))
                        drawPath(path, color = Color(0xFF00E5FF), style = Stroke(width = 4f))

                        val posterName = trackedImage?.name
                        val posterDrawings = DrawingStore.drawings[posterName]
                        android.util.Log.d("AR_DRAW", "Poster: $posterName, Drawings: ${posterDrawings?.size}")
                        android.util.Log.d("AR_DRAW", "Corner points: $cornerPoints")

                        if (!posterDrawings.isNullOrEmpty()) {
                            val quadOrigin = cornerPoints[0]
                            val quadRight = cornerPoints[1] - cornerPoints[0]
                            val quadDown = cornerPoints[3] - cornerPoints[0]

                            android.util.Log.d("AR_DRAW", "quadOrigin=$quadOrigin, quadRight=$quadRight, quadDown=$quadDown")

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
                text = message,
                modifier = Modifier.align(Alignment.BottomCenter).padding(40.dp),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
        }

    }

    private fun buildImageDatabase(session: Session): AugmentedImageDatabase {
        val db = AugmentedImageDatabase(session)
        val posters = listOf(
            "afis1.png" to 0.21f,
            "afis2.png" to 0.21f
        )

        posters.forEach { (name, width) ->
            try {
                assets.open(name).use { input ->
                    val bitmap = BitmapFactory.decodeStream(input)
                    db.addImage(name.removeSuffix(".png"), bitmap, width)
                }
            }
            catch (e: Exception) {
                android.util.Log.e("AR_ERROR", "Could not load asset: $name", e)
            }
        }
        return db
    }
}

fun isPointInQuad(point: Offset, quad: List<Offset>): Boolean {
    // Check if point is inside the polygon using ray casting
    var inside = false
    var j = quad.size - 1
    for (i in quad.indices) {
        val xi = quad[i].x; val yi = quad[i].y
        val xj = quad[j].x; val yj = quad[j].y
        val intersects = (yi > point.y) != (yj > point.y) &&
                point.x < (xj - xi) * (point.y - yi) / (yj - yi) + xi
        if (intersects) inside = !inside
        j = i
    }
    return inside
}


fun mapPathToQuad(
    path: Path,
    origin: Offset,       // TOP_LEFT screen point
    right: Offset,        // vector from TL to TR
    down: Offset          // vector from TL to BL
): Path {
    val matrix = android.graphics.Matrix()
    // Scale from [0,1] to the quad's actual screen size
    // This is a bilinear mapping approximated as an affine transform
    matrix.setValues(floatArrayOf(
        right.x, down.x, origin.x,
        right.y, down.y, origin.y,
        0f,      0f,     1f
    ))
//    val androidPath = path.asAndroidPath()
    val androidPath = android.graphics.Path(path.asAndroidPath()) // copy, not reference
    androidPath.transform(matrix)
    return Path().apply { addPath(androidPath.asComposePath()) }
}

fun mapNormalizedStrokeToQuad(
    points: List<Offset>,
    origin: Offset,
    right: Offset,
    down: Offset
): Path {
    return Path().apply {
        points.forEachIndexed { index, point ->
            // Bilinear mapping from [0,1] to quad
            val mapped = origin + right * point.x + down * point.y
            if (index == 0) moveTo(mapped.x, mapped.y)
            else lineTo(mapped.x, mapped.y)
        }
    }
}