package com.example.appmovilagenda

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Helper central para programar y cancelar recordatorios (AlarmManager).
 * Evita duplicación de código en Activities.
 */
object RecordatorioHelper {

    private const val ACTION_RECORDATORIO = "com.example.appmovilagenda.RECORDATORIO"

    /**
     * Crea el PendingIntent consistente para programar/cancelar.
     * Usamos el hash del id de la tarea como requestCode para garantizar unicidad.
     */
    private fun createPendingIntent(
        context: Context,
        tareaId: String,
        titulo: String? = null,
        flagsBase: Int = PendingIntent.FLAG_UPDATE_CURRENT
    ): PendingIntent {
        val intent = Intent(context, RecordatorioReceiver::class.java).apply {
            action = ACTION_RECORDATORIO
            putExtra("tareaId", tareaId)
            if (titulo != null) putExtra("titulo", titulo)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            flagsBase or PendingIntent.FLAG_IMMUTABLE
        else flagsBase
        val requestCode = tareaId.hashCode()
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    /**
     * Programa la alarma exacta (solo si millis está en el futuro).
     * Cancelamos primero cualquier alarma previa para la misma tarea.
     */
    fun programar(context: Context, tareaId: String, titulo: String, millis: Long) {
        val now = System.currentTimeMillis()
        if (millis <= now) {
            Log.w("RecordatorioHelper", "No se programa (pasado): tareaId=$tareaId millis=$millis now=$now")
            return
        }
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = createPendingIntent(context, tareaId, titulo)
        am.cancel(pi) // defensivo: limpia alarmas anteriores
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        Log.d("RecordatorioHelper", "Programado: tareaId=$tareaId titulo=\"$titulo\" at=$millis")
    }

    /**
     * Cancela la alarma asociada a la tarea.
     */
    fun cancelar(context: Context, tareaId: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = createPendingIntent(context, tareaId, null)
        am.cancel(pi)
        Log.d("RecordatorioHelper", "Cancelado: tareaId=$tareaId")
    }
}