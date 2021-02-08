package com.example.baenger

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.spotify.sdk.android.authentication.AuthenticationClient
import com.spotify.sdk.android.authentication.AuthenticationRequest
import com.spotify.sdk.android.authentication.AuthenticationResponse
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection


class MainActivity : AppCompatActivity() {

    var id = ""
    var displayName = ""
    var email = ""
    var avatar = ""
    var accessToken = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        val sharedPreferences = getSharedPreferences("BaengerPreferences", MODE_PRIVATE)
        val preferencesToken = sharedPreferences.getString("SpotifyToken", "")
        if (preferencesToken != null) {
            fetchSpotifyUserProfile(preferencesToken)
        }
        
        spotify_login_btn.setOnClickListener {
            val request = getAuthenticationRequest(AuthenticationResponse.Type.TOKEN)
            AuthenticationClient.openLoginActivity(
                    this,
                    SpotifyConstants.AUTH_TOKEN_REQUEST_CODE,
                    request
            )
        }

    }

    private fun getAuthenticationRequest(type: AuthenticationResponse.Type): AuthenticationRequest {
        return AuthenticationRequest.Builder(SpotifyConstants.CLIENT_ID, type, SpotifyConstants.REDIRECT_URI)
            .setShowDialog(false)
            .setScopes(arrayOf("user-read-email", "playlist-modify-public", "playlist-modify-private"))
            .build()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (SpotifyConstants.AUTH_TOKEN_REQUEST_CODE == requestCode) {
            val response = AuthenticationClient.getResponse(resultCode, data)
            val accessToken: String? = response.accessToken
            fetchSpotifyUserProfile(accessToken)
        }
    }

    private fun fetchSpotifyUserProfile(token: String?) {
        Log.d("Status: ", "Please Wait...")
        if (token == null) {
            Log.i("Status: ", "Something went wrong - No Access Token found")
            return
        }

        val getUserProfileURL = "https://api.spotify.com/v1/me"

        GlobalScope.launch(Dispatchers.Default) {
            val url = URL(getUserProfileURL)
            val httpsURLConnection = withContext(Dispatchers.IO) {url.openConnection() as HttpsURLConnection}
            httpsURLConnection.requestMethod = "GET"
            httpsURLConnection.setRequestProperty("Authorization", "Bearer $token")
            httpsURLConnection.doInput = true
            httpsURLConnection.doOutput = false
            val response = httpsURLConnection.inputStream.bufferedReader()
                .use { it.readText() }  // defaults to UTF-8
            withContext(Dispatchers.Main) {
                val jsonObject = JSONObject(response)

                // Spotify Id
                val spotifyId = jsonObject.getString("id")
                Log.d("Spotify Id :", spotifyId)
                id = spotifyId

                // Spotify Display Name
                val spotifyDisplayName = jsonObject.getString("display_name")
                Log.d("Spotify Display Name :", spotifyDisplayName)
                displayName = spotifyDisplayName

                // Spotify Email
                val spotifyEmail = jsonObject.getString("email")
                Log.d("Spotify Email :", spotifyEmail)
                email = spotifyEmail


                val spotifyAvatarArray = jsonObject.getJSONArray("images")
                //Check if user has Avatar
                var spotifyAvatarURL = ""
                if (spotifyAvatarArray.length() > 0) {
                    spotifyAvatarURL = spotifyAvatarArray.getJSONObject(0).getString("url")
                    Log.d("Spotify Avatar : ", spotifyAvatarURL)
                    avatar = spotifyAvatarURL
                }

                Log.d("Spotify AccessToken :", token)
                accessToken = token

                val sharedPreferences = getSharedPreferences("BaengerPreferences", MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString("SpotifyToken", token)
                editor.apply()

                openLoggedInActivity()
            }
        }
    }

    private fun openLoggedInActivity() {
        val myIntent = Intent(this@MainActivity, LoggedInActivity::class.java)
        myIntent.putExtra("spotify_id", id)
        myIntent.putExtra("spotify_display_name", displayName)
        myIntent.putExtra("spotify_email", email)
        myIntent.putExtra("spotify_avatar", avatar)
        myIntent.putExtra("spotify_access_token", accessToken)
        startActivity(myIntent)
    }


}