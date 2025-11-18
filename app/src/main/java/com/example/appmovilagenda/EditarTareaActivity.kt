package com.example.appmovilagenda

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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
        // Mantengo la constante por compatibilidad si alguna otra pantalla la envía,
        // pero en esta Activity se ignora por completo.
        const val EXTRA_RECORDATORIO_MILLIS = "extra_recordatorio_millis"
    }

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private var tareaId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_tarea)

        val edtTitulo = findViewById<TextInputEditText>(R.id.edtTitulo)
        val edtDescripcion = findViewById<TextInputEditText>(R.id.edtDescripcion)
        val edtFecha = findViewById<TextInputEditText>(R.id.edtFecha)
        val edtHora = findViewById<TextInputEditText>(R.id.edtHora)
        val btnGuardar = findViewById<MaterialButton>(R.id.btnGuardar)
        val btnCancelar = findViewById<MaterialButton>(R.id.btnCancelar)

        tareaId = intent.getStringExtra(EXTRA_ID).orEmpty()
        val titulo = intent.getStringExtra(EXTRA_TITULO).orEmpty()
        val descripcion = intent.getStringExtra(EXTRA_DESCRIPCION).orEmpty()
        val fecha = intent.getStringExtra(EXTRA_FECHA).orEmpty()
        val hora = intent.getStringExtra(EXTRA_HORA).orEmpty()

        if (tareaId.isBlank()) {
            Toast.makeText(this, "ID inválido", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        // Prefill
        edtTitulo.setText(titulo)
        edtDescripcion.setText(descripcion)
        edtFecha.setText(fecha)
        edtHora.setText(hora)

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

        btnGuardar.setOnClickListener {
            val nuevoTitulo = edtTitulo.text?.toString()?.trim().orEmpty()
            val nuevaDescripcion = edtDescripcion.text?.toString()?.trim().orEmpty()
            val nuevaFecha = edtFecha.text?.toString()?.trim().orEmpty()
            val nuevaHora = edtHora.text?.toString()?.trim().orEmpty()

            if (nuevoTitulo.isBlank()) {
                Toast.makeText(this, "Título obligatorio", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }

            val uid = auth.currentUser?.uid
            if (uid == null) {
                Toast.makeText(this, "Sesión inválida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Actualizamos campos y eliminamos el campo recordatorioMillis de Firestore
            val updateData = hashMapOf<String, Any>(
                "titulo" to nuevoTitulo,
                "descripcion" to nuevaDescripcion,
                "fecha" to nuevaFecha,
                "hora" to nuevaHora,
                "recordatorioMillis" to FieldValue.delete() // elimina el campo si existía
            )

            db.collection("todos").document(tareaId)
                .set(updateData, SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(this, "Cambios guardados", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        btnCancelar.setOnClickListener { finish() }
    }
}