package com.example.driveswipe

import android.service.notification.NotificationListenerService
import android.util.Log

class MediaNotificationListenerService : NotificationListenerService() {
    
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("DriveSwipe", "NotificationListenerService connected - granted global media key authority")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d("DriveSwipe", "NotificationListenerService disconnected")
    }
}
