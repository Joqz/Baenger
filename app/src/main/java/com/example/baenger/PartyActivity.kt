package com.example.baenger

import android.content.*
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.View.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.Track
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_party.*
import java.util.ArrayList


class PartyActivity : AppCompatActivity() {

    private val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
    private val ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"

    private var enableNotificationListenerAlertDialog: AlertDialog? = null

    private var username: String? = null
    private var playlistID: String? = null

    private var spotifyAppRemote: SpotifyAppRemote? = null

    private var notificationIntent: Intent? = null

    private var receiverRegistered: Boolean = false

    private var songsInQueue = ArrayList<String>()
    private var currentlyPlaying: String? = null
    private var queueCleared: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_party)
        supportActionBar?.hide()

        notificationIntent = Intent(this@PartyActivity, NotificationListener::class.java)

        val user = intent.getParcelableExtra<UserDetails>("userIntent")

        username = user?.username
        playlistID = user?.playlistID

        spotifyname_textview.text = username

        toggle_partymode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked){
                //Switch Button is Checked
                connectToSpotify()

                //move this into the if clause later
                spotify_clear_queue.visibility = VISIBLE
                if(songsInQueue.isNotEmpty()){

                }
            }
            else{
                //Switch Button is Unchecked
                stop()
                spotify_clear_queue.visibility = GONE
            }
        }

        spotify_clear_queue.setOnClickListener {
            queueCleared = false
            clearQueue(0)
        }
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

    private fun start(){
        spotifyAppRemote?.let {
            // Play a playlist
            val playlistURI = "spotify:playlist:$playlistID"
            it.playerApi.play(playlistURI)

            it.playerApi.subscribeToPlayerState().setEventCallback {
                val track: Track = it.track
                currentlyPlaying = track.uri

                Log.d("CURRENTLY", "Changing")
                if(songsInQueue.contains(track.uri)){
                    songsInQueue.remove(track.uri)
                    Log.d("STARTED", "started playing from queue, removed song")
                }


                Log.d("MUSIC THAT IS PLAYING", track.name + " by " + track.artist.name)
                Log.d("SongURI", track.uri)


            }
        }
        Log.d("LoggedInActivity", "MUSIC SHOULD START PLAYING")
        Log.d("LoggedInActivity", "spotify:playlist:$playlistID")

        if(!isNotificationServiceEnabled()){
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

    private val SongReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action

            if (action == "SongID Broadcast") {
                val receivedID = intent.getStringExtra("SongID")
                val songURI = "spotify:track:$receivedID"
                checkIfSongExistsInQueue(songURI)
            }
        }
    }

    private fun buildNotificationServiceAlertDialog(): AlertDialog {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("aNNA OikeuKSET")
        alertDialogBuilder.setMessage("tänne ne datat")
        alertDialogBuilder.setPositiveButton("Yes") { _, _ -> startActivity(Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        alertDialogBuilder.setNegativeButton("No") { _, _ ->
            // If you choose to not enable the notification listener
            // the app. will not work as expected
        }
        return alertDialogBuilder.create()
    }

    private fun checkIfSongExistsInQueue(songURI: String){
        Log.d("QUEUE", songsInQueue.toString())

        if(currentlyPlaying != songURI){
            if(songsInQueue.contains(songURI)){
                Log.d("NOPE", "cant add, in queue")
            }
            else{
                Log.d("ADDING", songURI)
                addToQueue(songURI)
            }
        }
        else{
            Log.d("PLAYING", "cant add, currently playing")
        }
    }

    private fun addToQueue(songURI: String) {
        songsInQueue.add(songURI)
        spotifyAppRemote?.playerApi?.queue(songURI)
    }

    private fun clearQueue(i: Int){
        val checkSong = "spotify:track:15SgAfCwXlyxMNPFgWkAlc"

        if(i == 0){
            queueCleared = false
            songsInQueue.clear()
            Log.d("ARRAY", "cleared")

            spotifyAppRemote?.playerApi?.queue(checkSong)
            Log.d("TESTSONG", "added")
        }
        if(currentlyPlaying == checkSong) {
            Log.d("QUEUE", "cleared")
            queueCleared = true
            spotifyAppRemote?.playerApi?.skipNext()
        }
        if(!queueCleared){
            Log.d("QUEUE", "not clear")
            spotifyAppRemote?.playerApi?.skipNext()
            val handler = Handler()
            val runnable = Runnable {
                clearQueue(1)
            }

            handler.postDelayed(runnable, 1000)
        }
    }







}
