package dev.jovanni0.itec19.audio

import android.content.Context
import android.media.MediaPlayer
import dev.jovanni0.itec19.data.Team
import dev.jovanni0.itec19.stores.AudioStore

object TeamAudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var currentTeam: Team? = null

    fun play(context: Context, team: Team?) {
        if (team == null) { stop(); return }
        if (currentTeam == team) return

        stop()
        val resId = AudioStore.teamAnthems[team] ?: return
        currentTeam = team
        mediaPlayer = MediaPlayer.create(context, resId)?.apply {
            isLooping = true
            start()
        }
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        currentTeam = null
    }
}