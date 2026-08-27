package com.gymos.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class HidratacionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "hidratacion_channel",
                "Recordatorio de hidratación",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Recordatorios para tomar agua"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, "hidratacion_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("💧 ¡Hora de tomar agua!")
            .setContentText("Recordá mantenerte hidratado durante el día.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefsConfig = context.getSharedPreferences("gymos_config", Context.MODE_PRIVATE)
            if (prefsConfig.getBoolean("recordatorio_agua", false)) {
                val horas = prefsConfig.getInt("recordatorio_horas", 2)
                programarRecordatorio(context, horas)
            }
        }
    }
}

fun programarRecordatorio(context: Context, horas: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
    val intent = Intent(context, HidratacionReceiver::class.java)
    val pendingIntent = android.app.PendingIntent.getBroadcast(
        context, 0, intent,
        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
    )
    val intervalo = horas * 60 * 60 * 1000L
    alarmManager.setRepeating(
        android.app.AlarmManager.RTC_WAKEUP,
        System.currentTimeMillis() + intervalo,
        intervalo,
        pendingIntent
    )
}

fun cancelarRecordatorio(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
    val intent = Intent(context, HidratacionReceiver::class.java)
    val pendingIntent = android.app.PendingIntent.getBroadcast(
        context, 0, intent,
        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
    pendingIntent.cancel()
}