package dev.jovanni0.itec19.ar

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.google.ar.core.AugmentedImage
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import dev.jovanni0.itec19.PosterDetailActivity
import io.github.sceneview.ar.ARScene

@Composable
fun ArOverlayScene(onPosterDetected: (String) -> Unit) {
    var trackedImage by remember { mutableStateOf<AugmentedImage?>(null) }
    var cornerPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        ARScene(
            modifier = Modifier.fillMaxSize(),
            onSessionCreated = { session -> configureSession(context, session) },
            onSessionUpdated = { _, frame ->
                val visible = frame.getUpdatedTrackables(AugmentedImage::class.java)
                    .firstOrNull {
                        it.trackingState == TrackingState.TRACKING &&
                                it.trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING
                    }

                trackedImage = visible

                if (visible != null) {
                    onPosterDetected(visible.name)
                    cornerPoints = projectCorners(visible, frame.camera, canvasSize)
                } else {
                    cornerPoints = emptyList()
                }
            }
        )

        Box(modifier = Modifier.fillMaxSize().onSizeChanged { canvasSize = it })

        if (cornerPoints.size == 4) {
            DrawingOverlay(
                cornerPoints = cornerPoints,
                posterName = trackedImage?.name,
                onTap = { handleTap(context, trackedImage) }
            )
        }

        Text(
            text = "Scan a poster",
            modifier = Modifier.align(Alignment.BottomCenter).padding(40.dp),
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )
    }
}

private fun handleTap(context: Context, trackedImage: AugmentedImage?) {
    val posterName = trackedImage?.name ?: return
    context.startActivity(
        Intent(context, PosterDetailActivity::class.java).apply {
            putExtra("poster_name", posterName)
        }
    )
}

private fun configureSession(context: Context, session: Session) {
    session.configure(Config(session).apply {
        augmentedImageDatabase = buildImageDatabase(context, session)
        focusMode = Config.FocusMode.AUTO
        updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
    })
}