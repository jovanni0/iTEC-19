package dev.jovanni0.itec19.stores

import androidx.compose.runtime.mutableStateMapOf
import dev.jovanni0.itec19.data.Sticker



object StickerStore {
    val stickers = mutableStateMapOf<String, List<Sticker>>()
}