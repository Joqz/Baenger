package com.example.baenger

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.baenger.NotificationListener.InterceptedNotificationCode


class NotificationListener: NotificationListenerService()  {

    object InterceptedNotificationCode {
        public var separatedID : String? = null
        const val WHATSAPP_CODE = 2
        const val INSTAGRAM_CODE = 3
        const val OTHER_NOTIFICATIONS_CODE = 4 // We ignore all notification with code == 4
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pack = sbn.packageName
        var ticker = ""
        if (sbn.notification.tickerText != null) {
            ticker = sbn.notification.tickerText.toString()
        }
        val extras = sbn.notification.extras
        val title = extras.getString("android.title")
        val text = extras.getCharSequence("android.text").toString()
        if (title != null) {
            Log.d("Title", title)
        }
        Log.d("Text", text)

        val testString = "https://open.spotify.com/track/"

        if(text.contains(testString)){
            InterceptedNotificationCode.separatedID = text.substringAfter(
                "https://open.spotify.com/track/").substringBefore("?si=")
            Log.d("SONG ID", InterceptedNotificationCode.separatedID!!)

            val intent = Intent("test")
            intent.putExtra("SongID", InterceptedNotificationCode.separatedID)
            sendBroadcast(intent)
            Log.d("broadcast sent", intent.toString())
            InterceptedNotificationCode.separatedID = null
        }
    }
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Implement what you want here
    }
}