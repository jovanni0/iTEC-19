package dev.jovanni0.itec19.stores

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import dev.jovanni0.itec19.DrawConfig
import dev.jovanni0.itec19.data.Team

object DrawingStore {
    val drawings = mutableStateMapOf<String, List<Pair<List<Offset>, DrawConfig>>>()
    val dominance = mutableStateMapOf<String, Team?>()

    fun getLastStrokeId(posterId: String): String?
    {
        return this.drawings[posterId]?.lastOrNull()?.second?.strokeId
    }
}