package com.example.baenger

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.spotify.sdk.android.authentication.AuthenticationClient
import com.spotify.sdk.android.authentication.AuthenticationResponse
import kotlinx.android.synthetic.main.activity_loggedin.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection


class LoggedInActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val spotifyId = intent.getStringExtra("spotify_id")
        val spotifyDisplayName = intent.getStringExtra("spotify_display_name")
        val spotifyEmail = intent.getStringExtra("spotify_email")
        val spotifyAvatarURL = intent.getStringExtra("spotify_avatar")
        val spotifyAccessToken = intent.getStringExtra("spotify_access_token")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loggedin)
        supportActionBar?.hide()

        spotifyname_textview.text = spotifyDisplayName

        spotify_create_playlist.setOnClickListener {createPlaylist(spotifyAccessToken, spotifyId)}
    }

    private fun createPlaylist(spotifyAccessToken: String?, spotifyId: String?) {
        Log.d("Status: ", "Please Wait...")
        if (spotifyAccessToken == null) {
            Log.i("Status: ", "Something went wrong - No Access Token found")
            return
        }

        val getplaylistURL = "https://api.spotify.com/v1/users/" + spotifyId + "/playlists"

        GlobalScope.launch(Dispatchers.Default) {
            val url = URL(getplaylistURL)
            val httpsURLConnection = withContext(Dispatchers.IO) {url.openConnection() as HttpsURLConnection }
            httpsURLConnection.requestMethod = "POST"
            httpsURLConnection.setRequestProperty("Authorization", "Bearer $spotifyAccessToken")
            httpsURLConnection.setRequestProperty("Accept", "application/json")
            httpsURLConnection.setRequestProperty("Content-Type", "application/json")
            httpsURLConnection.doInput = true
            httpsURLConnection.doOutput = true

            val requestJson = JSONObject("""{"name":"Baenger", "description":"Playlist for Baenger","public":"false"}""")

            val os: OutputStream = httpsURLConnection.getOutputStream()
            os.write(requestJson.toString().toByteArray())
            os.close()

            val response = httpsURLConnection.inputStream.bufferedReader()
                .use { it.readText() }  // defaults to UTF-8
            withContext(Dispatchers.Main) {
                val jsonObject = JSONObject(response)
            }
        }
    }
}