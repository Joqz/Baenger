package com.example.baenger

import android.content.*
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.View.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.PlayerState
import com.spotify.protocol.types.Track
import kotlinx.android.synthetic.main.activity_loggedin.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_party.*
import kotlinx.android.synthetic.main.activity_party.spotifyname_textview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileNotFoundException
import java.io.IOException
import java.io.OutputStream
import java.net.URL
import java.util.ArrayList
import java.util.stream.Collectors
import java.util.stream.Stream
import javax.net.ssl.HttpsURLConnection


class PartyActivity : AppCompatActivity() {

    private val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
    private val ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"

    private var enableNotificationListenerAlertDialog: AlertDialog? = null
    private var playlistDialog: AlertDialog? = null

    private var username: String? = null
    private var playlistID: String? = null
    private var accessToken: String? = null

    private var spotifyAppRemote: SpotifyAppRemote? = null

    private var notificationIntent: Intent? = null

    private var receiverRegistered: Boolean = false

    private var songsInQueue = ArrayList<String>()
    private var allSongs = ArrayList<String>()
    private var songsInPlaylist = ArrayList<String>()
    private var currentlyPlaying: String? = null
    private var queueCleared: Boolean = false

    var preferencesTimer: Long? = null
    var preferencesToken: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_party)
        supportActionBar?.hide()

        val sharedPreferences = getSharedPreferences("BaengerPreferences", MODE_PRIVATE)
        preferencesTimer = sharedPreferences.getLong("ExpiredDate", -1)
        preferencesToken = sharedPreferences.getString("SpotifyToken", "")

        notificationIntent = Intent(this@PartyActivity, NotificationListener::class.java)

        val user = intent.getParcelableExtra<UserDetails>("userIntent")

        username = user?.username
        playlistID = user?.playlistID
        accessToken = user?.token

        spotifyname_textview.text = username

        playButton.setOnClickListener {
            PlayerStateControl.resume()

        }

        pauseButton.setOnClickListener {
            PlayerStateControl.pause()
        }

        skipButton.setOnClickListener {
            PlayerStateControl.skipNext()
        }



        toggle_partymode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                //Switch Button is Checked
                connectToSpotify()
                getPlaylist()

                spotify_clear_queue.visibility = VISIBLE
            } else {
                //Switch Button is Unchecked
                stop()
                spotify_clear_queue.visibility = INVISIBLE

                if (!emptyCheck(allSongs)) {
                    playlistDialog = buildPlaylistAlertDialog()
                    playlistDialog?.show()
                }

            }
        }

        spotify_clear_queue.setOnClickListener {
            queueCleared = false
            clearQueue(0)
        }
    }

    private fun emptyCheck(arr: ArrayList<String>): Boolean {
        return arr.isEmpty()
    }

    override fun onDestroy() {
        super.onDestroy()
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
        }

        if (receiverRegistered) {
            unregisterReceiver(SongReceiver)
            receiverRegistered = false
        }
    }

    //Remote connect to Spotify app
    private fun connectToSpotify() {

        val connectionParams = ConnectionParams.Builder(SpotifyConstants.CLIENT_ID)
                .setRedirectUri(SpotifyConstants.REDIRECT_URI)
                .showAuthView(true)
                .build()

        SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                Log.d("LoggedInActivity", "Connected! Yay!")
                // Now you can start interacting with App Remote
                start()
            }

            override fun onFailure(throwable: Throwable) {
                Log.e("LoggedInActivity", throwable.message, throwable)
                // Something went wrong when attempting to connect! Handle errors here
            }
        })
    }

    //Disconnect from the Spotify app & notification listener
    private fun stop() {
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
        }

        Intent(this, NotificationListener::class.java).also { intent ->
            stopService(intent)
        }

        unregisterReceiver(SongReceiver)
        receiverRegistered = false
    }

    //Start the Party Mode
    private fun start() {

        spotifyAppRemote?.let {
            // Play a playlist
            val playlistURI = "spotify:playlist:$playlistID"
            it.playerApi.play(playlistURI)

            it.playerApi.subscribeToPlayerState().setEventCallback {
                val track: Track = it.track
                currentlyPlaying = track.uri

                Log.d("CURRENTLY", "Changing")
                if (songsInQueue.contains(track.uri)) {
                    songsInQueue.remove(track.uri)
                    Log.d("STARTED", "started playing from queue, removed song")
                }


                Log.d("MUSIC THAT IS PLAYING", track.name + " by " + track.artist.name)
                Log.d("IMAGEURI", track.imageUri.toString())
                Log.d("SongURI", track.uri)

                Glide.with(this)
                        .load("https://i.scdn.co/image/" + track.imageUri.toString().substring(22, track.imageUri.toString().length - 2))
                        .placeholder(R.drawable.spotify_icon)
                        .into(imageview)
                trackname.text = track.name
                artist.text = track.artist.name


            }
        }
        Log.d("LoggedInActivity", "MUSIC SHOULD START PLAYING")
        Log.d("LoggedInActivity", "spotify:playlist:$playlistID")

        if (!isNotificationServiceEnabled()) {
            enableNotificationListenerAlertDialog = buildNotificationServiceAlertDialog();
            enableNotificationListenerAlertDialog?.show();
        }

        Intent(this, NotificationListener::class.java).also { intent ->
            startService(intent)
        }

        fun getIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction("SongID Broadcast")
            return intentFilter
        }

        registerReceiver(SongReceiver, getIntentFilter())
        receiverRegistered = true
    }

    //Check if the app is allowed to read notifications
    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName

        val flat: String = Settings.Secure.getString(contentResolver, ENABLED_NOTIFICATION_LISTENERS)
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":").toTypedArray()
            for (i in names.indices) {
                val cn = ComponentName.unflattenFromString(names[i])
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.packageName)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    //Receiver to receive broadcasts each time a notification is created
    private val SongReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action

            if (action == "SongID Broadcast") {
                val receivedID = intent.getStringExtra("SongID")
                val songURI = "spotify:track:$receivedID"
                checkQueue(songURI)
            }
        }
    }

    //Checking if the song is already in the queue
    private fun checkQueue(songURI: String) {
        Log.d("QUEUE", songsInQueue.toString())

        if (currentlyPlaying != songURI) {
            if (songsInQueue.contains(songURI)) {
                Log.d("NOPE", "cant add, in queue")
            } else {
                Log.d("ADDING", songURI)
                addToQueue(songURI)
            }
        } else {
            Log.d("PLAYING", "cant add, currently playing")
        }
    }

    //Obviously add the song to queue
    private fun addToQueue(songURI: String) {
        songsInQueue.add(songURI)
        spotifyAppRemote?.playerApi?.queue(songURI)

        if (!allSongs.contains(songURI)) {
            if (!songsInPlaylist.contains(songURI)) {
                allSongs.add("'" + songURI + "'")
                Log.d("ALLSONGS", allSongs.toString())
            }
        }

    }

    //Clearing the queue by adding a song to the end of the queue and skipping songs until its reached
    private fun clearQueue(i: Int) {
        val checkSong = "spotify:track:15SgAfCwXlyxMNPFgWkAlc"

        if (i == 0) {
            queueCleared = false
            songsInQueue.clear()
            Log.d("ARRAY", "cleared")

            spotifyAppRemote?.playerApi?.queue(checkSong)
            Log.d("TESTSONG", "added")
        }
        if (currentlyPlaying == checkSong) {
            Log.d("QUEUE", "cleared")
            queueCleared = true
            spotifyAppRemote?.playerApi?.skipNext()
        }
        if (!queueCleared) {
            Log.d("QUEUE", "not clear")
            spotifyAppRemote?.playerApi?.skipNext()
            val handler = Handler()
            val runnable = Runnable {
                clearQueue(1)
            }

            handler.postDelayed(runnable, 1000)
        }
    }

    private fun addToPlaylist(songs: ArrayList<String>) {
        //The songs should be filtered well enough earlier

        Log.d("ADDING", songs.toString())

        val getplaylistURL = "https://api.spotify.com/v1/playlists/$playlistID/tracks"

        GlobalScope.launch(Dispatchers.Default) {
            val url = URL(getplaylistURL)
            val httpsURLConnection = withContext(Dispatchers.IO) { url.openConnection() as HttpsURLConnection }
            httpsURLConnection.requestMethod = "POST"
            httpsURLConnection.setRequestProperty("Authorization", "Bearer $preferencesToken")
            httpsURLConnection.setRequestProperty("Accept", "application/json")
            httpsURLConnection.setRequestProperty("Content-Type", "application/json")
            httpsURLConnection.doInput = true
            httpsURLConnection.doOutput = true

            val requestJson = JSONObject("""{"uris": $songs, "position": 0}""")

            Log.d("REQUe", requestJson.toString())

            try {
                val os: OutputStream = httpsURLConnection.getOutputStream()
                os.write(requestJson.toString().toByteArray())
                os.close()
            } catch (e: FileNotFoundException) {
                e.message?.let { Log.d("ERROR", it) }
            }

            val response = httpsURLConnection.inputStream.bufferedReader()
                    .use { it.readText() }  // defaults to UTF-8
            withContext(Dispatchers.Main) {
                val jsonObject = JSONObject(response)

                Log.d("RESPONSE", jsonObject.toString())
            }

            allSongs.clear()
        }

    }

    private fun getPlaylist() {
        val checkPlaylistURL = "https://api.spotify.com/v1/playlists/$playlistID"

        songsInPlaylist.clear()

        GlobalScope.launch(Dispatchers.Default) {
            val url = URL(checkPlaylistURL)
            val httpsURLConnection = withContext(Dispatchers.IO) { url.openConnection() as HttpsURLConnection }
            httpsURLConnection.requestMethod = "GET"
            httpsURLConnection.setRequestProperty("Authorization", "Bearer $accessToken")
            httpsURLConnection.setRequestProperty("Accept", "application/json")
            httpsURLConnection.setRequestProperty("Content-Type", "application/json")
            httpsURLConnection.doInput = true
            httpsURLConnection.doOutput = false

            val response = httpsURLConnection.inputStream.bufferedReader()
                    .use { it.readText() }  // defaults to UTF-8
            withContext(Dispatchers.Main) {
                val jsonObject = JSONObject(response).getJSONObject("tracks")
                val jsonArray = jsonObject.getJSONArray("items")

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray[i] as JSONObject
                    val trackObject = item.getJSONObject("track")
                    val trackURI = trackObject.getString("uri")
                    Log.i("TRACKURI: ", trackURI)

                    songsInPlaylist.add(trackURI)
                }
            }
        }
    }

    //Dialog to ask the user if we can read their notifications
    private fun buildNotificationServiceAlertDialog(): AlertDialog {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Request to read your notifications")
        alertDialogBuilder.setMessage("We need to access your notifications to be able to activate " +
                "party mode. Do you accept?")
        alertDialogBuilder.setPositiveButton("Yes") { _, _ ->
            startActivity(Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        alertDialogBuilder.setNegativeButton("No") { _, _ ->
        }
        return alertDialogBuilder.create()
    }

    //Dialog to ask the user if they want to add songs to the playlist
    private fun buildPlaylistAlertDialog(): AlertDialog {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Want to add the songs to your playlist?")
        alertDialogBuilder.setMessage("By accepting, we will add the songs from this session" +
                "to your Baenger playlist.")
        alertDialogBuilder.setPositiveButton("Yes") { _, _ ->
            addToPlaylist(allSongs)
        }
        alertDialogBuilder.setNegativeButton("No") { _, _ ->
            allSongs.clear()
        }
        return alertDialogBuilder.create()
    }

}
