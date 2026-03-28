package dev.jovanni0.itec19.data

import androidx.compose.ui.graphics.Color

enum class Team(
    val displayName: String,
    val color: Color
) {
    RED("Red", Color.Red),
    GREEN("Green", Color(0xFF00C853)),
    BLUE("Blue", Color(0xFF2196F3))
}