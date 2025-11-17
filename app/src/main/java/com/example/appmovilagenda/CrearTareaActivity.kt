package com.example.appmovilagenda

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

        private const val MARGEN_TEST_MS = 30_000L          // Permite pruebas cercanas (>=30s)
        private const val DOS_DIAS_MS = 2L * 24 * 60 * 60 * 1000
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
        val btnAgregar = findViewById<MaterialButton>(R.id.btnAgregar)

        // Fecha entrega
        edtFecha.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                val c = Calendar.getInstance()
                c.set(y, m, d, 0, 0, 0)
                c.set(Calendar.MILLISECOND, 0)
                edtFecha.setText(formatoFecha.format(c.time))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Hora entrega
        edtHora.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, h, min ->
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, min)
                edtHora.setText(formatoHora.format(cal.time))
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        btnAgregar.setOnClickListener {
            val titulo = edtTitulo.text?.toString()?.trim().orEmpty()
            val descripcion = edtDescripcion.text?.toString()?.trim().orEmpty()
            val fecha = edtFecha.text?.toString()?.trim().orEmpty()
            val hora = edtHora.text?.toString()?.trim().orEmpty()

            if (titulo.isBlank()) { toast("Título obligatorio"); return@setOnClickListener }
            if (fecha.isBlank())  { toast("Debes elegir la fecha de entrega"); return@setOnClickListener }

            val dueMillis = parseDueMillis(fecha, hora)
            if (dueMillis == null) { toast("Fecha u hora de entrega inválida"); return@setOnClickListener }

            showReminderChoiceDialog(dueMillis) { recMsOrNull ->
                recordatorioMillis = recMsOrNull

                // Validaciones finales
                recordatorioMillis?.let { recMs ->
                    val now = System.currentTimeMillis()
                    if (recMs < now + MARGEN_TEST_MS) {
                        toast("El recordatorio debe ser al menos 30 segundos en el futuro")
                        return@showReminderChoiceDialog
                    }
                    if (recMs >= dueMillis) {
                        toast("El recordatorio debe ser antes de la entrega")
                        return@showReminderChoiceDialog
                    }
                }

                // Devolvemos resultado
                intent.putExtra(EXTRA_TITULO, titulo)
                intent.putExtra(EXTRA_DESCRIPCION, descripcion)
                intent.putExtra(EXTRA_FECHA, fecha)
                intent.putExtra(EXTRA_HORA, hora)
                intent.putExtra(EXTRA_RECORDATORIO_MILLIS, recordatorioMillis ?: -1L)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }
    }

    private fun showReminderChoiceDialog(dueMillis: Long, onResult: (Long?) -> Unit) {
        val opciones = arrayOf("2 días antes (recomendado)", "Elegir manualmente", "No agregar")
        MaterialAlertDialogBuilder(this)
            .setTitle("¿Deseas agregar un recordatorio?")
            .setItems(opciones) { dialog, idx ->
                when (idx) {
                    0 -> { // 2 días antes
                        val candidato = dueMillis - DOS_DIAS_MS
                        val now = System.currentTimeMillis()
                        if (candidato <= now + MARGEN_TEST_MS) {
                            toast("Entrega muy próxima, elige hora manual.")
                            pickManualReminder(dueMillis) { onResult(it) }
                        } else onResult(candidato)
                    }
                    1 -> pickManualReminder(dueMillis) { onResult(it) }
                    else -> onResult(null)
                }
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun pickManualReminder(dueMillis: Long, onPicked: (Long?) -> Unit) {
        val now = System.currentTimeMillis()
        var sugerido = dueMillis - DOS_DIAS_MS
        val unaHoraDespues = now + 60 * 60 * 1000
        if (sugerido < unaHoraDespues) sugerido = unaHoraDespues

        val def = Calendar.getInstance().apply { timeInMillis = sugerido }

        DatePickerDialog(this, { _, y, m, d ->
            TimePickerDialog(this, { _, h, min ->
                val cal = Calendar.getInstance().apply {
                    set(y, m, d, h, min, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onPicked(cal.timeInMillis)
            }, def.get(Calendar.HOUR_OF_DAY), def.get(Calendar.MINUTE), true).show()
        }, def.get(Calendar.YEAR), def.get(Calendar.MONTH), def.get(Calendar.DAY_OF_MONTH)).show()
    }

    // Si no hay hora de entrega, asumimos 23:59 del día elegido
    private fun parseDueMillis(fechaTxt: String, horaTxt: String?): Long? {
        return try {
            val p = fechaTxt.split("-")
            val d = p[0].toInt()
            val m = p[1].toInt() - 1
            val y = p[2].toInt()
            val cal = Calendar.getInstance()
            if (horaTxt.isNullOrBlank()) cal.set(y, m, d, 23, 59, 0)
            else {
                val hh = horaTxt.split(":")
                cal.set(y, m, d, hh[0].toInt(), hh[1].toInt(), 0)
            }
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        } catch (_: Exception) { null }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}