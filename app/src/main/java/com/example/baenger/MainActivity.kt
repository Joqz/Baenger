package com.example.baenger

import android.content.Intent
import android.os.Bundle
import android.util.Base64
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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL
import java.security.KeyStore
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
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

        spotify_login_btn.setOnClickListener {
            val request = getAuthenticationRequest(AuthenticationResponse.Type.TOKEN)
            AuthenticationClient.openLoginActivity(
                    this,
                    SpotifyConstants.AUTH_TOKEN_REQUEST_CODE,
                    request
            )
        }
    }

    private fun getKey() : SecretKey{
        val keystore = KeyStore.getInstance("AndroidKeyStore")
        keystore.load(null)

        val secretKeyEntry = keystore.getEntry("SpotifyToken", null) as KeyStore.SecretKeyEntry
        return secretKeyEntry.secretKey
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

        val ks = KeyStore.getInstance(KeyStore.getDefaultType())
        try {
            FileInputStream("BaengerKeyStore").use { fis -> ks.load(fis, null)}
        }catch (t: Throwable){
            ks.load(null, null);
            FileOutputStream("BaengerKeyStore").use { fos -> ks.store(fos, null) }
        }



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

                Log.d("STORING TO KEYSTORE: ", token)
                val encodedToken: ByteArray = Base64.decode(token, Base64.DEFAULT)
                val secretKeyToken: SecretKey = SecretKeySpec(encodedToken, 0, encodedToken.size, "AES")
                val keyStoreEntry: KeyStore.Entry = KeyStore.SecretKeyEntry(secretKeyToken)

                val password = "".toCharArray()
                val protParam: KeyStore.ProtectionParameter = KeyStore.PasswordProtection(password)

                ks.setEntry("SpotifyToken", keyStoreEntry, protParam)

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