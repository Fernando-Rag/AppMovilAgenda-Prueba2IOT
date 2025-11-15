package com.example.appmovilagenda

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class InicioTareasActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tareaAdapter: TareaAdapter

    // Lista mutable para poder agregar nuevas tareas
    private val listaTareas = mutableListOf(
        Tarea("Ir al gimnasio", "Entrenar piernas", "18-11-2025", "10:00"),
        Tarea("Estudiar Android", "RecyclerView + Layouts", "18-11-2025", "14:00"),
        Tarea("Hacer compras", "Comprar pan y leche", "18-11-2025", "19:00")
    )

    // Recibir resultado del formulario
    private val crearTareaLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult
                val titulo = data.getStringExtra(CrearTareaActivity.EXTRA_TITULO).orEmpty()
                val descripcion = data.getStringExtra(CrearTareaActivity.EXTRA_DESCRIPCION).orEmpty()
                val fecha = data.getStringExtra(CrearTareaActivity.EXTRA_FECHA).orEmpty()
                val hora = data.getStringExtra(CrearTareaActivity.EXTRA_HORA).orEmpty()

                if (titulo.isNotEmpty()) {
                    listaTareas.add(Tarea(titulo, descripcion, fecha, hora))
                    tareaAdapter.actualizarLista(listaTareas)
                    recyclerView.scrollToPosition(listaTareas.lastIndex)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio_tareas)

        recyclerView = findViewById(R.id.recyclerTareas)
        recyclerView.layoutManager = LinearLayoutManager(this)
        tareaAdapter = TareaAdapter(listaTareas)
        recyclerView.adapter = tareaAdapter

        val fab: FloatingActionButton = findViewById(R.id.btnAgregar)
        fab.setOnClickListener {
            val intent = Intent(this, CrearTareaActivity::class.java)
            crearTareaLauncher.launch(intent)
        }
    }
}
