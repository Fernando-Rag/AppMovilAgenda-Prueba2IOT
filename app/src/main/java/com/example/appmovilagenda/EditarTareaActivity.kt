package com.example.appmovilagenda

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class EditarTareaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "extra_id"
        const val EXTRA_TITULO = "extra_titulo"
        const val EXTRA_DESCRIPCION = "extra_descripcion"
        const val EXTRA_FECHA = "extra_fecha"
        const val EXTRA_HORA = "extra_hora"
        const val EXTRA_RECORDATORIO_MILLIS = "extra_recordatorio_millis"
    }

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private var recordatorioMillis: Long? = null
    private var tareaId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_tarea)

        val edtTitulo = findViewById<TextInputEditText>(R.id.edtTitulo)
        val edtDescripcion = findViewById<TextInputEditText>(R.id.edtDescripcion)
        val edtFecha = findViewById<TextInputEditText>(R.id.edtFecha)
        val edtHora = findViewById<TextInputEditText>(R.id.edtHora)
        val edtRecFecha = findViewById<TextInputEditText>(R.id.edtRecordatorioFecha)
        val edtRecHora = findViewById<TextInputEditText>(R.id.edtRecordatorioHora)
        val btnGuardar = findViewById<MaterialButton>(R.id.btnGuardar)
        val btnCancelar = findViewById<MaterialButton>(R.id.btnCancelar)

        tareaId = intent.getStringExtra(EXTRA_ID).orEmpty()
        val titulo = intent.getStringExtra(EXTRA_TITULO).orEmpty()
        val descripcion = intent.getStringExtra(EXTRA_DESCRIPCION).orEmpty()
        val fecha = intent.getStringExtra(EXTRA_FECHA).orEmpty()
        val hora = intent.getStringExtra(EXTRA_HORA).orEmpty()
        val recMillis = intent.getLongExtra(EXTRA_RECORDATORIO_MILLIS, -1L)
        if (recMillis > 0) recordatorioMillis = recMillis

        if (tareaId.isBlank()) {
            Toast.makeText(this, "ID inválido", Toast.LENGTH_SHORT).show(); finish(); return
        }

        // Prefill
        edtTitulo.setText(titulo)
        edtDescripcion.setText(descripcion)
        edtFecha.setText(fecha)
        edtHora.setText(hora)
        recordatorioMillis?.let {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it
            val d = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))
            val m = String.format("%02d", cal.get(Calendar.MONTH) + 1)
            val y = cal.get(Calendar.YEAR)
            val h = String.format("%02d", cal.get(Calendar.HOUR_OF_DAY))
            val min = String.format("%02d", cal.get(Calendar.MINUTE))
            edtRecFecha.setText("$d-$m-$y")
            edtRecHora.setText("$h:$min")
        }

        edtFecha.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, mm, dd ->
                val dia = String.format("%02d", dd)
                val mes = String.format("%02d", mm + 1)
                edtFecha.setText("$dia-$mes-$y")
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        edtHora.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, h, min ->
                val hs = String.format("%02d", h)
                val ms = String.format("%02d", min)
                edtHora.setText("$hs:$ms")
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        edtRecFecha.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, mm, dd ->
                val dia = String.format("%02d", dd)
                val mes = String.format("%02d", mm + 1)
                edtRecFecha.setText("$dia-$mes-$y")
                if (edtRecHora.text?.toString()?.isNotBlank() == true) {
                    recalcularRecordatorio(edtRecFecha.text.toString(), edtRecHora.text.toString())
                }
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        edtRecHora.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, h, min ->
                val hs = String.format("%02d", h)
                val ms = String.format("%02d", min)
                edtRecHora.setText("$hs:$ms")
                if (edtRecFecha.text?.toString()?.isNotBlank() == true) {
                    recalcularRecordatorio(edtRecFecha.text.toString(), edtRecHora.text.toString())
                }
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        btnGuardar.setOnClickListener {
            val nuevoTitulo = edtTitulo.text?.toString()?.trim().orEmpty()
            val nuevaDescripcion = edtDescripcion.text?.toString()?.trim().orEmpty()
            val nuevaFecha = edtFecha.text?.toString()?.trim().orEmpty()
            val nuevaHora = edtHora.text?.toString()?.trim().orEmpty()

            if (nuevoTitulo.isBlank()) {
                Toast.makeText(this, "Título obligatorio", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            recordatorioMillis?.let {
                if (it <= System.currentTimeMillis()) {
                    Toast.makeText(this, "Recordatorio debe ser futuro", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            val uid = auth.currentUser?.uid
            if (uid == null) {
                Toast.makeText(this, "Sesion inválida", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }

            val updateMap = mutableMapOf<String, Any>(
                "titulo" to nuevoTitulo,
                "descripcion" to nuevaDescripcion,
                "fecha" to nuevaFecha,
                "hora" to nuevaHora
            )
            if (recordatorioMillis != null) {
                updateMap["recordatorioMillis"] = recordatorioMillis!!
            } else {
                updateMap["recordatorioMillis"] = null as Any
            }

            db.collection("todos").document(tareaId)
                .update(updateMap)
                .addOnSuccessListener {
                    // Reprogramar notificación
                    cancelarRecordatorio(tareaId)
                    recordatorioMillis?.let { millis ->
                        if (millis > System.currentTimeMillis()) {
                            programarRecordatorio(tareaId, nuevoTitulo, millis)
                        }
                    }
                    Toast.makeText(this, "Cambios guardados", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        btnCancelar.setOnClickListener { finish() }
    }

    private fun recalcularRecordatorio(fechaTxt: String, horaTxt: String) {
        try {
            val partesF = fechaTxt.split("-")
            val partesH = horaTxt.split(":")
            if (partesF.size == 3 && partesH.size == 2) {
                val d = partesF[0].toInt()
                val m = partesF[1].toInt() - 1
                val y = partesF[2].toInt()
                val h = partesH[0].toInt()
                val min = partesH[1].toInt()
                val cal = Calendar.getInstance()
                cal.set(y, m, d, h, min, 0)
                cal.set(Calendar.MILLISECOND, 0)
                recordatorioMillis = cal.timeInMillis
            }
        } catch (_: Exception) {
            recordatorioMillis = null
        }
    }

    private fun programarRecordatorio(tareaId: String, titulo: String, millis: Long) {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, RecordatorioReceiver::class.java).apply {
            putExtra("tareaId", tareaId)
            putExtra("titulo", titulo)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        val pi = PendingIntent.getBroadcast(this, tareaId.hashCode(), intent, flags)
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
    }

    private fun cancelarRecordatorio(tareaId: String) {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, RecordatorioReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        val pi = PendingIntent.getBroadcast(this, tareaId.hashCode(), intent, flags)
        am.cancel(pi)
    }
}