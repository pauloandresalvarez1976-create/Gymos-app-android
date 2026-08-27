package com.gymos.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class PodometroService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var pasos = 0
    private var umbral = 12f
    private var ultimaMagnitud = 0f
    private var enPico = false
    private var ultimoPaso = 0L

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("gymos_podometro", MODE_PRIVATE)
        umbral = prefs.getFloat("umbral", 12f)
        startForeground(2001, crearNotificacion(0))
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST) }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
        val magnitud = kotlin.math.sqrt(x * x + y * y + z * z)
        val ahora = System.currentTimeMillis()
        if (magnitud > umbral && ultimaMagnitud <= umbral && !enPico) {
            if (ahora - ultimoPaso > 250L) {
                enPico = true
                pasos++
                ultimoPaso = ahora
                actualizarNotificacion()
            }
        } else if (magnitud < umbral - 2f) {
            enPico = false
        }
        ultimaMagnitud = magnitud
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun crearNotificacion(pasos: Int): Notification {
        val channelId = "podometro_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Podómetro", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("🚶 Podómetro activo")
            .setContentText("$pasos pasos registrados")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    private fun actualizarNotificacion() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(2001, crearNotificacion(pasos))
        // Enviar broadcast a la app
        sendBroadcast(Intent("com.gymos.app.PASOS_UPDATE").putExtra("pasos", pasos))
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}