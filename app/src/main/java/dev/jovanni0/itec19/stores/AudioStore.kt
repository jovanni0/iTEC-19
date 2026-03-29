package dev.jovanni0.itec19.stores

import dev.jovanni0.itec19.R
import dev.jovanni0.itec19.data.Team


object AudioStore {
    val teamAnthems = mapOf(
        Team.RED   to R.raw.anthem,
        Team.GREEN to R.raw.anthem2,
        Team.BLUE  to R.raw.anthem3
    )
}