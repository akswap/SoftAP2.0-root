package com.example.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class RouterWebServerService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    companion object {
        const val CHANNEL_ID = "router_web_server_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START_SERVER"
        const val ACTION_STOP = "ACTION_STOP_SERVER"
        private const val TAG = "RouterWebServerService"

        var activeServer: EmbeddedRouterServer? = null

        fun isServerRunning(): Boolean {
            return activeServer?.isServerRunning() == true
        }

        fun startService(context: Context, server: EmbeddedRouterServer? = null) {
            if (server != null) {
                activeServer = server
            }
            try {
                val intent = Intent(context, RouterWebServerService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting foreground service", e)
            }
        }

        fun stopService(context: Context) {
            try {
                activeServer?.stop()
                activeServer = null
                val intent = Intent(context, RouterWebServerService::class.java).apply {
                    action = ACTION_STOP
                }
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping service", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireLocks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            activeServer?.stop()
            activeServer = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                }
                startForeground(NOTIFICATION_ID, notification, serviceType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallback to standard startForeground", e)
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (ex: Exception) {
                Log.e(TAG, "Failed startForeground completely", ex)
            }
        }

        acquireLocks()
        activeServer?.let { server ->
            if (!server.isServerRunning()) {
                server.start()
            }
        }

        return START_STICKY
    }

    private fun acquireLocks() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (wakeLock == null) {
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "PocoHotspot::RouterWebServerWakeLock"
                ).apply {
                    setReferenceCounted(false)
                    acquire(24 * 60 * 60 * 1000L) // 24 hour max acquire lock
                }
                Log.i(TAG, "CPU Partial WakeLock acquired for screen-off web server operation")
            }

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (wifiLock == null) {
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                } else {
                    @Suppress("DEPRECATION")
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF
                }
                wifiLock = wifiManager.createWifiLock(mode, "PocoHotspot::RouterWebServerWifiLock").apply {
                    setReferenceCounted(false)
                    acquire()
                }
                Log.i(TAG, "WiFi High Performance Lock acquired for screen-off web server operation")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring WakeLock/WifiLock", e)
        }
    }

    private fun releaseLocks() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
            wakeLock = null

            wifiLock?.let {
                if (it.isHeld) it.release()
            }
            wifiLock = null
            Log.i(TAG, "WakeLock and WifiLock released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing WakeLock/WifiLock", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SoftAP Web Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps SoftAP Web Server active when phone screen is off"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, RouterWebServerService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStopIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SoftAP Web Server Active")
            .setContentText("Server active on port 8080 (Screen Off Protection Active)")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentIntent(pendingOpenIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Server",
                pendingStopIntent
            )
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        activeServer?.stop()
        activeServer = null
        releaseLocks()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
