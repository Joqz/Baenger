package com.example.baenger

import android.content.*
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.Track
import kotlinx.android.synthetic.main.activity_party.*


class PartyActivity : AppCompatActivity() {

    private val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
    private val ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"

    private var enableNotificationListenerAlertDialog: AlertDialog? = null

    private var username: String? = null
    private var playlistID: String? = null

    private var spotifyAppRemote: SpotifyAppRemote? = null

    private var notificationIntent: Intent? = null

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
            }
            else{
                //Switch Button is Unchecked
                stop()
            }
        }
    }

    /*override fun onStart(){
        super.onStart()
        connectToSpotify()
    }*/

    override fun onDestroy() {
        super.onDestroy()
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
        }
        unregisterReceiver(SongReceiver)
        // Aaand we will finish off here.
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
    }

    private fun start(){
        spotifyAppRemote?.let {
            // Play a playlist
            val playlistURI = "spotify:playlist:$playlistID"
            it.playerApi.play(playlistURI)

            it.playerApi.subscribeToPlayerState().setEventCallback {
                val track: Track = it.track
                Log.d("MUSIC THAT IS PLAYING", track.name + " by " + track.artist.name)
            }
        }
        Log.d("LoggedInActivity", "MUSIC SHOULD START PLAYING")
        Log.d("LoggedInActivity", "spotify:playlist$playlistID")

        if(!isNotificationServiceEnabled()){
            enableNotificationListenerAlertDialog = buildNotificationServiceAlertDialog();
            enableNotificationListenerAlertDialog?.show();
        }

        Intent(this, NotificationListener::class.java).also { intent ->
            startService(intent)
        }

        /*receiver = BroadCastReceiver()

        val filter = IntentFilter()
        filter.addAction("test")
        registerReceiver(receiver, filter)
        Log.d("registerReceiver", "yes")*/

        fun getIntentFilter(): IntentFilter {
            val iFilter = IntentFilter()
            iFilter.addAction("test")
            return iFilter
        }

        registerReceiver(SongReceiver, getIntentFilter())

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

    private fun buildNotificationServiceAlertDialog(): AlertDialog? {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("aNNA OikeuKSET")
        alertDialogBuilder.setMessage("tänne ne datat")
        alertDialogBuilder.setPositiveButton("Yes") { dialog, id -> startActivity(Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        alertDialogBuilder.setNegativeButton("No") { dialog, id ->
            // If you choose to not enable the notification listener
            // the app. will not work as expected
        }
        return alertDialogBuilder.create()
    }

    /*private val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            Log.d("onReceive", "yes")
            val receivedID = intent!!.getStringExtra("SongID")
            if (receivedID != null) {
                addSongToQueue(receivedID)
            }
        }
    }*/

    private fun addSongToQueue(receivedID: String) {
        Log.d("addSongToQueue", "yes")
        spotifyAppRemote?.playerApi?.play("spotify:track:$receivedID")
    }

    private val SongReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("onReceive", "yes")
            val action = intent?.action

            if (action == "test") {
                val receivedID = intent.getStringExtra("SongID")
                if (receivedID != null) {
                    addSongToQueue(receivedID)
                }
            }
        }
    }







}
