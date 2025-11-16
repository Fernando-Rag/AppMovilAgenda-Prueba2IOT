package com.example.appmovilagenda

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class RecordatorioReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "recordatorios_tareas"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val tareaId = intent.getStringExtra("tareaId").orEmpty()
        val titulo = intent.getStringExtra("titulo").orEmpty()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Recordatorio")
            .setContentText("¡Es hora de: $titulo!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(tareaId.hashCode(), notification)
    }
}