package dev.jovanni0.itec19.ar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import dev.jovanni0.itec19.DrawingStore
import kotlin.collections.get

@Composable
fun DrawingOverlay(
    cornerPoints: List<Offset>,
    posterName: String?,
    onTap: () -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    if (cornerPoints.size == 4 && isPointInQuad(tapOffset, cornerPoints)) {
                        onTap()
                    }
                }
            }
    ) {
        val outlinePath = Path().apply {
            moveTo(cornerPoints[0].x, cornerPoints[0].y)
            lineTo(cornerPoints[1].x, cornerPoints[1].y)
            lineTo(cornerPoints[2].x, cornerPoints[2].y)
            lineTo(cornerPoints[3].x, cornerPoints[3].y)
            close()
        }

        drawPath(outlinePath, color = Color(0x440000FF))
        drawPath(outlinePath, color = Color(0xFF00E5FF), style = Stroke(width = 4f))

        val drawings = DrawingStore.drawings[posterName]
        if (!drawings.isNullOrEmpty()) {
            val quadOrigin = cornerPoints[0]
            val quadRight = cornerPoints[1] - cornerPoints[0]
            val quadDown = cornerPoints[3] - cornerPoints[0]

            drawings.forEach { (points, config) ->
                drawPath(
                    path = mapNormalizedStrokeToQuad(points, quadOrigin, quadRight, quadDown),
                    color = config.color,
                    style = Stroke(width = config.strokeWidth)
                )
            }
        }
    }
}