package com.example.baenger

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import com.spotify.sdk.android.authentication.AuthenticationClient
import com.spotify.sdk.android.authentication.AuthenticationRequest
import com.spotify.sdk.android.authentication.AuthenticationResponse
import kotlinx.android.synthetic.main.activity_loggedin.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class PlayActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        val spotifyId = intent.getStringExtra("spotify_id")
        val spotifyDisplayName = intent.getStringExtra("spotify_display_name")
        val spotifyEmail = intent.getStringExtra("spotify_email")
        val spotifyAvatarURL = intent.getStringExtra("spotify_avatar")
        val spotifyAccessToken = intent.getStringExtra("spotify_access_token")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        supportActionBar?.hide()

        spotifyname_textview.text = spotifyDisplayName
    }


}