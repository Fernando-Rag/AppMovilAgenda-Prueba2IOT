package com.example.appmovilagenda

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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
            Toast.makeText(this, "ID inválido", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        // Prefill
        edtTitulo.setText(titulo)
        edtDescripcion.setText(descripcion)
        edtFecha.setText(fecha)
        edtHora.setText(hora)
        recordatorioMillis?.let {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it
            val d = "%02d".format(cal.get(Calendar.DAY_OF_MONTH))
            val m = "%02d".format(cal.get(Calendar.MONTH) + 1)
            val y = cal.get(Calendar.YEAR)
            val h = "%02d".format(cal.get(Calendar.HOUR_OF_DAY))
            val min = "%02d".format(cal.get(Calendar.MINUTE))
            edtRecFecha.setText("$d-$m-$y")
            edtRecHora.setText("$h:$min")
        }

        edtFecha.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, mm, dd ->
                edtFecha.setText("%02d-%02d-%d".format(dd, mm + 1, y))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        edtHora.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, h, min ->
                edtHora.setText("%02d:%02d".format(h, min))
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        edtRecFecha.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, mm, dd ->
                edtRecFecha.setText("%02d-%02d-%d".format(dd, mm + 1, y))
                val hTxt = edtRecHora.text?.toString().orEmpty()
                if (hTxt.isNotBlank()) recalcularRecordatorio(edtRecFecha.text.toString(), hTxt)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        edtRecHora.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, h, min ->
                edtRecHora.setText("%02d:%02d".format(h, min))
                val fTxt = edtRecFecha.text?.toString().orEmpty()
                if (fTxt.isNotBlank()) recalcularRecordatorio(fTxt, edtRecHora.text.toString())
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
                    Toast.makeText(this, "El recordatorio debe ser futuro", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            val uid = auth.currentUser?.uid
            if (uid == null) {
                Toast.makeText(this, "Sesión inválida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updateData = hashMapOf(
                "titulo" to nuevoTitulo,
                "descripcion" to nuevaDescripcion,
                "fecha" to nuevaFecha,
                "hora" to nuevaHora,
                "recordatorioMillis" to recordatorioMillis
            )

            db.collection("todos").document(tareaId)
                .set(updateData, SetOptions.merge())
                .addOnSuccessListener {
                    // Reprogramar
                    RecordatorioHelper.cancelar(this, tareaId)
                    recordatorioMillis?.let { millis ->
                        if (millis > System.currentTimeMillis()) {
                            RecordatorioHelper.programar(this, tareaId, nuevoTitulo, millis)
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
            val f = fechaTxt.split("-")
            val h = horaTxt.split(":")
            if (f.size == 3 && h.size == 2) {
                val d = f[0].toInt()
                val m = f[1].toInt() - 1
                val y = f[2].toInt()
                val hh = h[0].toInt()
                val mm = h[1].toInt()
                val cal = Calendar.getInstance()
                cal.set(y, m, d, hh, mm, 0)
                cal.set(Calendar.MILLISECOND, 0)
                recordatorioMillis = cal.timeInMillis
            } else {
                recordatorioMillis = null
            }
        } catch (_: Exception) {
            recordatorioMillis = null
        }
    }
}