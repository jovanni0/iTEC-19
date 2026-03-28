package dev.jovanni0.itec19.ar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath


fun isPointInQuad(point: Offset, quad: List<Offset>): Boolean
{
    var inside = false
    var j = quad.size - 1

    for (i in quad.indices)
    {
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