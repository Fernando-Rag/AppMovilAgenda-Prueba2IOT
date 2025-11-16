package com.example.appmovilagenda

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CrearTareaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TITULO = "extra_titulo"
        const val EXTRA_DESCRIPCION = "extra_descripcion"
        const val EXTRA_FECHA = "extra_fecha"
        const val EXTRA_HORA = "extra_hora"
        const val EXTRA_RECORDATORIO_MILLIS = "extra_recordatorio_millis"
    }

    private val formatoFecha = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    private val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())

    private var recordatorioMillis: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_tarea)

        val edtTitulo = findViewById<TextInputEditText>(R.id.edtTitulo)
        val edtDescripcion = findViewById<TextInputEditText>(R.id.edtDescripcion)
        val edtFecha = findViewById<TextInputEditText>(R.id.edtFecha)
        val edtHora = findViewById<TextInputEditText>(R.id.edtHora)
        val edtRecFecha = findViewById<TextInputEditText>(R.id.edtRecordatorioFecha)
        val edtRecHora = findViewById<TextInputEditText>(R.id.edtRecordatorioHora)
        val btnAgregar = findViewById<MaterialButton>(R.id.btnAgregar)

        edtFecha.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                val c = Calendar.getInstance()
                c.set(y, m, d, 0, 0, 0)
                edtFecha.setText(formatoFecha.format(c.time))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        edtHora.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, h, min ->
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, min)
                edtHora.setText(formatoHora.format(cal.time))
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        edtRecFecha.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                val c = Calendar.getInstance()
                c.set(y, m, d, 0, 0, 0)
                edtRecFecha.setText(formatoFecha.format(c.time))
                // Recalcular recordatorioMillis si ya hay hora
                val horaTxt = edtRecHora.text?.toString().orEmpty()
                if (horaTxt.isNotBlank()) {
                    setRecordatorioMillis(edtRecFecha.text.toString(), horaTxt)
                }
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        edtRecHora.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, h, min ->
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, min)
                edtRecHora.setText(formatoHora.format(cal.time))
                val fechaTxt = edtRecFecha.text?.toString().orEmpty()
                if (fechaTxt.isNotBlank()) {
                    setRecordatorioMillis(fechaTxt, edtRecHora.text.toString())
                }
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        btnAgregar.setOnClickListener {
            val titulo = edtTitulo.text?.toString()?.trim().orEmpty()
            val descripcion = edtDescripcion.text?.toString()?.trim().orEmpty()
            val fecha = edtFecha.text?.toString()?.trim().orEmpty()
            val hora = edtHora.text?.toString()?.trim().orEmpty()
            val recFecha = edtRecFecha.text?.toString()?.trim().orEmpty()
            val recHora = edtRecHora.text?.toString()?.trim().orEmpty()

            if (titulo.isBlank()) {
                Toast.makeText(this, "Título obligatorio", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }

            // Si el usuario puso recordatorio fecha y hora pero no se calculó, lo calculamos
            if (recFecha.isNotBlank() && recHora.isNotBlank() && recordatorioMillis == null) {
                setRecordatorioMillis(recFecha, recHora)
            }

            // Validar que el recordatorio sea futuro si existe
            recordatorioMillis?.let {
                if (it <= System.currentTimeMillis()) {
                    Toast.makeText(this, "El recordatorio debe ser a futuro", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            intent.putExtra(EXTRA_TITULO, titulo)
            intent.putExtra(EXTRA_DESCRIPCION, descripcion)
            intent.putExtra(EXTRA_FECHA, fecha)
            intent.putExtra(EXTRA_HORA, hora)
            intent.putExtra(EXTRA_RECORDATORIO_MILLIS, recordatorioMillis ?: -1L)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    private fun setRecordatorioMillis(fechaTxt: String, horaTxt: String) {
        try {
            val partesFecha = fechaTxt.split("-")
            val partesHora = horaTxt.split(":")
            if (partesFecha.size == 3 && partesHora.size == 2) {
                val d = partesFecha[0].toInt()
                val m = partesFecha[1].toInt() - 1
                val y = partesFecha[2].toInt()
                val h = partesHora[0].toInt()
                val min = partesHora[1].toInt()
                val cal = Calendar.getInstance()
                cal.set(y, m, d, h, min, 0)
                cal.set(Calendar.MILLISECOND, 0)
                recordatorioMillis = cal.timeInMillis
            }
        } catch (_: Exception) {
            recordatorioMillis = null
        }
    }
}