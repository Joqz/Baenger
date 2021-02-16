package com.example.baenger

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import kotlinx.android.synthetic.main.activity_loggedin.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection


class LoggedInActivity : AppCompatActivity() {

    private var playlistID: String? = null
    private var user: UserDetails? = null

    var playlistCheck = false;
    var baengerFound = false;

    override fun onCreate(savedInstanceState: Bundle?) {
        val spotifyId = intent.getStringExtra("spotify_id")
        val spotifyDisplayName = intent.getStringExtra("spotify_display_name")
        val spotifyEmail = intent.getStringExtra("spotify_email")
        val spotifyAvatarURL = intent.getStringExtra("spotify_avatar")
        val spotifyAccessToken = intent.getStringExtra("spotify_access_token")

        val sharedPreferences = getSharedPreferences("BaengerPreferences", MODE_PRIVATE)
        val preferencesToken = sharedPreferences.getString("SpotifyToken", "")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loggedin)
        supportActionBar?.hide()

        spotifyname_textview.text = spotifyDisplayName

        //CHECKING FOR THE BAENGER PLAYLIST

        Log.d("Status: ", "Please Wait...")
        if (spotifyAccessToken == null) {
            Log.i("Status: ", "Something went wrong - No Access Token found")
            return
        }

        val checkforplaylistsURL = "https://api.spotify.com/v1/me/playlists"

        GlobalScope.launch(Dispatchers.Default) {
            val url = URL(checkforplaylistsURL + "?limit=50&offset=0")
            val httpsURLConnection = withContext(Dispatchers.IO) {url.openConnection() as HttpsURLConnection }
            httpsURLConnection.requestMethod = "GET"
            httpsURLConnection.setRequestProperty("Authorization", "Bearer $spotifyAccessToken")
            httpsURLConnection.setRequestProperty("Accept", "application/json")
            httpsURLConnection.setRequestProperty("Content-Type", "application/json")
            httpsURLConnection.doInput = true
            httpsURLConnection.doOutput = false

            val response = httpsURLConnection.inputStream.bufferedReader()
                .use { it.readText() }  // defaults to UTF-8
            withContext(Dispatchers.Main) {
                val jsonArray = JSONObject(response).get("items") as JSONArray

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray[i] as JSONObject
                    val playlistName = item.getString("name")
                    Log.i("Names: ", playlistName)

                    if (playlistName == "Baenger"){
                        baengerFound = true;
                        playlistID = item.getString("id")
                        Log.i("Status: ", "BAENGER PLAYLIST FOUND, NOTHING SHOULD HAPPEN")
                    }
                }
                playlistCheck = true;

                if(playlistCheck && baengerFound){
                    user = UserDetails(spotifyAccessToken, spotifyId, spotifyDisplayName, playlistID)
                    //CHANGE ACTIVITY HERE!!
                    changeActivity()
                }
                if(playlistCheck && !baengerFound){
                    spotify_create_playlist.setOnClickListener {createPlaylist(spotifyAccessToken, spotifyId)}
                }

            }
        }
    }

    private fun connected() {
        // Then we will write some more code here.

        //Commenting this out since it is currently unnecessary but we might need it later

        /*spotifyAppRemote?.let {
            // Play a playlist
            val playlistURI = "spotify:playlist:$playlistID"
            it.playerApi.play(playlistURI)

            it.playerApi.subscribeToPlayerState().setEventCallback {
                val track: Track = it.track
                Log.d("MUSIC THAT IS PLAYING", track.name + " by " + track.artist.name)
            }
        }
        Log.d("LoggedInActivity", "MUSIC SHOULD START PLAYING")
        Log.d("LoggedInActivity", "spotify:playlist$playlistID")*/

    }

    private fun createPlaylist(spotifyAccessToken: String?, spotifyId: String?) {

        val getplaylistURL = "https://api.spotify.com/v1/users/$spotifyId/playlists"

        GlobalScope.launch(Dispatchers.Default) {
            val url = URL(getplaylistURL)
            val httpsURLConnection = withContext(Dispatchers.IO) { url.openConnection() as HttpsURLConnection }
            httpsURLConnection.requestMethod = "POST"
            httpsURLConnection.setRequestProperty("Authorization", "Bearer $spotifyAccessToken")
            httpsURLConnection.setRequestProperty("Accept", "application/json")
            httpsURLConnection.setRequestProperty("Content-Type", "application/json")
            httpsURLConnection.doInput = true
            httpsURLConnection.doOutput = true

            val requestJson = JSONObject("""{"name":"Baenger", "description":"Playlist for Baenger","public":"true"}""")

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

    private fun changeActivity() {
        val userIntent = Intent(this@LoggedInActivity, PartyActivity::class.java)
        userIntent.putExtra("userIntent", user)
        startActivity(userIntent)
    }
}
