package dev.jovanni0.itec19.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import okhttp3.MultipartBody

object StabilityApi
{
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val API_KEY = dev.jovanni0.itec19.BuildConfig.STABILITY_API_KEY


    suspend fun generateSticker(prompt: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val fullPrompt = "$prompt, sticker style, white background, thick outline, cute, high quality, digital art"
            val negativePrompt = "blurry, dark background, realistic photo, ugly"

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("prompt", fullPrompt)
                .addFormDataPart("negative_prompt", negativePrompt)
                .addFormDataPart("model", "sd3.5-flash")
                .addFormDataPart("aspect_ratio", "1:1")
                .addFormDataPart("output_format", "png")
                .build()

            val request = Request.Builder()
                .url("https://api.stability.ai/v2beta/stable-image/generate/sd3")
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Accept", "image/*")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                android.util.Log.e("StabilityApi", "Error: ${response.body?.string()}")
                return@withContext null
            }

            val imageBytes = response.body?.bytes() ?: return@withContext null
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        } catch (e: Exception) {
            android.util.Log.e("StabilityApi", "Failed to generate image", e)
            null
        }
    }
}