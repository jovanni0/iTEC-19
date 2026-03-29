package dev.jovanni0.itec19.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toArgb
import dev.jovanni0.itec19.DrawConfig
import dev.jovanni0.itec19.data.Sticker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.graphics.createBitmap


suspend fun savePosterAsPng(
    context: Context,
    posterName: String,
    posterBitmap: Bitmap,
    paths: List<Pair<List<Offset>, DrawConfig>>,
    stickers: List<Sticker>
) {
    Log.d("State", "Triggered save as PNG")

    withContext(Dispatchers.IO) {
        val width = posterBitmap.width
        val height = posterBitmap.height
        val output = createBitmap(width, height)
        val canvas = android.graphics.Canvas(output)

        // draw poster
        canvas.drawBitmap(posterBitmap, 0f, 0f, null)

        // draw strokes
        val size = Size(width.toFloat(), height.toFloat())
        paths.forEach { (points, config) ->
            val paint = android.graphics.Paint().apply {
                color = config.color.toArgb()
                strokeWidth = config.strokeWidth
                style = android.graphics.Paint.Style.STROKE
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
                isAntiAlias = true
            }
            val androidPath = android.graphics.Path()
            points.map { it.denormalize(size) }.forEachIndexed { i, offset ->
                if (i == 0) androidPath.moveTo(offset.x, offset.y)
                else androidPath.lineTo(offset.x, offset.y)
            }
            canvas.drawPath(androidPath, paint)
        }

        // draw stickers
        stickers.forEach { sticker ->
            val bytes = Base64.decode(sticker.data, Base64.NO_WRAP)
            val stickerBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            val stickerSize = width * 0.15f
            val x = sticker.position.x * width - stickerSize / 2
            val y = sticker.position.y * height - stickerSize / 2
            val dst = android.graphics.RectF(x, y, x + stickerSize, y + stickerSize)
            canvas.drawBitmap(stickerBmp, null, dst, null)
        }

        // save to gallery
        val filename = "${posterName}_${System.currentTimeMillis()}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                output.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
        } ?: Log.d("Save", "URI is null - insert failed")
    }
    Log.d("Save", "Save complete")
}