package dev.jovanni0.itec19.ar

import android.content.Context
import android.graphics.BitmapFactory
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Session



fun buildImageDatabase(context: Context, session: Session): AugmentedImageDatabase
{
    val db = AugmentedImageDatabase(session)
    val posters = listOf(
        "afis1.png" to 0.21f,
        "afis2.png" to 0.21f,
//            "afis3.png" to 0.21f,
//            "afis4.png" to 0.21f,
//            "afis5.png" to 0.21f,
//            "afis6.png" to 0.21f,
//            "afis7.png" to 0.21f,
//            "afis8.png" to 0.21f,
//            "afis9.png" to 0.21f,
//            "afis10.png" to 0.21f
    )

    posters.forEach { (name, width) ->
        try {
            context.assets.open(name).use { input ->
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