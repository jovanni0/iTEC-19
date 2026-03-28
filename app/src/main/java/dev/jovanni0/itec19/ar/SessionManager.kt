package dev.jovanni0.itec19.ar

import android.opengl.Matrix
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.google.ar.core.AugmentedImage
import com.google.ar.core.Camera
import com.google.ar.core.Pose

fun projectCorners(
    image: AugmentedImage,
    camera: Camera,
    canvasSize: IntSize
): List<Offset> {
    return try {
        val projectionMatrix = FloatArray(16)
        val viewMatrix = FloatArray(16)
        camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)
        camera.getViewMatrix(viewMatrix, 0)

        val halfW = image.extentX / 2f
        val halfH = image.extentZ / 2f

        listOf(
            Pose.makeTranslation(-halfW, 0f, -halfH), // TOP_LEFT
            Pose.makeTranslation( halfW, 0f, -halfH), // TOP_RIGHT
            Pose.makeTranslation( halfW, 0f,  halfH), // BOTTOM_RIGHT
            Pose.makeTranslation(-halfW, 0f,  halfH)  // BOTTOM_LEFT
        ).mapNotNull { translation ->
            val worldPos = image.centerPose.compose(translation)
                .let { floatArrayOf(it.tx(), it.ty(), it.tz(), 1f) }

            val viewPos = FloatArray(4).also { Matrix.multiplyMV(it, 0, viewMatrix, 0, worldPos, 0) }
            val clipPos = FloatArray(4).also { Matrix.multiplyMV(it, 0, projectionMatrix, 0, viewPos, 0) }

            if (clipPos[3] == 0f) return@mapNotNull null

            Offset(
                x = (clipPos[0] / clipPos[3] + 1f) / 2f * canvasSize.width,
                y = (1f - clipPos[1] / clipPos[3]) / 2f * canvasSize.height
            )
        }
    } catch (e: Exception) {
        android.util.Log.e("AR_OVERLAY", "Corner projection failed", e)
        emptyList()
    }
}