package com.example.baenger

import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.SpotifyAppRemote

enum class PlayingState {
    PAUSED, PLAYING, STOPPED
}

object PlayerStateControl {

    private var spotifyAppRemote: SpotifyAppRemote? = null



    fun play(uri: String) {
        spotifyAppRemote?.playerApi?.play(uri)
    }

    fun resume() {
        spotifyAppRemote?.playerApi?.resume()
    }

    fun pause() {
        spotifyAppRemote?.playerApi?.pause()
    }

    fun skipNext() {
        spotifyAppRemote?.playerApi?.skipNext()
    }

}