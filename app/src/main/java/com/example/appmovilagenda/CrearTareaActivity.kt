package com.example.appmovilagenda

import android.app.Activity
import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar
import java.util.Locale

class CrearTareaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TITULO = "extra_titulo"
        const val EXTRA_DESCRIPCION = "extra_descripcion"
        const val EXTRA_FECHA = "extra_fecha"
        const val EXTRA_HORA = "extra_hora" // opcional; lo enviaremos vacío
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_tarea)

        val edtTitulo = findViewById<TextInputEditText>(R.id.edtTitulo)
        val edtDescripcion = findViewById<TextInputEditText>(R.id.edtDescripcion)
        val edtFecha = findViewById<TextInputEditText>(R.id.edtFecha)
        val btnAgregar = findViewById<MaterialButton>(R.id.btnAgregar)

        // Abrir DatePicker al tocar el campo fecha
        edtFecha.setOnClickListener {
            val cal = Calendar.getInstance()
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH)
            val d = cal.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, year, month, day ->
                // Formato dd-MM-yyyy
                val dd = String.format(Locale.getDefault(), "%02d", day)
                val mm = String.format(Locale.getDefault(), "%02d", month + 1)
                edtFecha.setText("$dd-$mm-$year")
            }, y, m, d).show()
        }

        btnAgregar.setOnClickListener {
            val titulo = edtTitulo.text?.toString()?.trim().orEmpty()
            val descripcion = edtDescripcion.text?.toString()?.trim().orEmpty()
            val fecha = edtFecha.text?.toString()?.trim().orEmpty()

            if (titulo.isEmpty()) {
                Toast.makeText(this, "El título es obligatorio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Devolvemos los datos a InicioTareasActivity
            intent.putExtra(EXTRA_TITULO, titulo)
            intent.putExtra(EXTRA_DESCRIPCION, descripcion)
            intent.putExtra(EXTRA_FECHA, fecha)
            intent.putExtra(EXTRA_HORA, "") // sin hora (tu lista la mostrará vacía)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }
}