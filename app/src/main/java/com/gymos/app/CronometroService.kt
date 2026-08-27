package com.gymos.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class CronometroService : Service() {

    private var milisegundos = 0L
    private var corriendo = false
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        startForeground(2002, crearNotificacion("00:00"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> iniciar()
            "PAUSE" -> corriendo = false
            "STOP" -> { corriendo = false; stopSelf() }
        }
        return START_STICKY
    }

    private fun iniciar() {
        corriendo = true
        scope.launch {
            while (corriendo) {
                delay(100L)
                milisegundos += 100
                val min = (milisegundos / 60000).toInt()
                val seg = ((milisegundos % 60000) / 1000).toInt()
                val tiempo = "%02d:%02d".format(min, seg)
                actualizarNotificacion(tiempo)
                sendBroadcast(Intent("com.gymos.app.CRONOMETRO_UPDATE").putExtra("milisegundos", milisegundos))
            }
        }
    }

    private fun crearNotificacion(tiempo: String): Notification {
        val channelId = "cronometro_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Cronómetro", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("⏱️ Cronómetro activo")
            .setContentText(tiempo)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setOngoing(true)
            .build()
    }

    private fun actualizarNotificacion(tiempo: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(2002, crearNotificacion(tiempo))
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}