package com.example.appmovilagenda

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FECHA = "extra_fecha_dia" // formato dd-MM-yyyy
    }

    private lateinit var chipMes: TextView
    private lateinit var chipSemana: TextView
    private lateinit var chipDia: TextView

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: TareaAdapter

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private var listener: ListenerRegistration? = null

    private val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    private lateinit var fechaSeleccionada: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dia)
        title = "Día"

        // Fecha recibida o hoy por defecto
        fechaSeleccionada = intent.getStringExtra(EXTRA_FECHA)
            ?: sdf.format(Date())

        chipMes = findViewById(R.id.chipMes)
        chipSemana = findViewById(R.id.chipSemana)
        chipDia = findViewById(R.id.chipDia)

        setChipSelected(chipMes, false)
        setChipSelected(chipSemana, false)
        setChipSelected(chipDia, true)

        chipMes.setOnClickListener {
            startActivity(Intent(this, CalendarioActivity::class.java))
        }
        chipSemana.setOnClickListener {
            startActivity(Intent(this, SemanaActivity::class.java))
        }
        chipDia.setOnClickListener { /* ya aquí */ }

        recycler = findViewById(R.id.recyclerTareasDia)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = TareaAdapter(emptyList()) { tarea ->
            val intent = Intent(this, EditarTareaActivity::class.java).apply {
                putExtra(EditarTareaActivity.EXTRA_ID, tarea.id)
                putExtra(EditarTareaActivity.EXTRA_TITULO, tarea.titulo)
                putExtra(EditarTareaActivity.EXTRA_DESCRIPCION, tarea.descripcion)
                putExtra(EditarTareaActivity.EXTRA_FECHA, tarea.fecha)
                putExtra(EditarTareaActivity.EXTRA_HORA, tarea.hora)
                putExtra(EditarTareaActivity.EXTRA_RECORDATORIO_MILLIS, tarea.recordatorioMillis ?: -1L)
            }
            startActivity(intent)
        }
        recycler.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Inicia sesión para ver tus tareas", Toast.LENGTH_SHORT).show()
            adapter.actualizarLista(emptyList())
            return
        }

        // Solo tareas del usuario y de la fecha seleccionada
        listener = db.collection("todos")
            .whereEqualTo("userId", uid)
            .whereEqualTo("fecha", fechaSeleccionada)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                val tareas = snapshot?.documents?.map { doc ->
                    Tarea(
                        id = doc.id,
                        titulo = doc.getString("titulo").orEmpty(),
                        descripcion = doc.getString("descripcion").orEmpty(),
                        fecha = doc.getString("fecha").orEmpty(),
                        hora = doc.getString("hora").orEmpty(),
                        recordatorioMillis = doc.getLong("recordatorioMillis"),
                        createdAt = doc.getTimestamp("createdAt"),
                        userId = doc.getString("userId").orEmpty()
                    )
                }.orEmpty()
                adapter.actualizarLista(tareas)
            }
    }

    override fun onStop() {
        super.onStop()
        listener?.remove()
        listener = null
    }

    private fun setChipSelected(view: TextView, selected: Boolean) {
        if (selected) {
            view.setBackgroundResource(R.drawable.chip_brown)
            view.setTextColor(0xFF6B4E2E.toInt())
        } else {
            view.setBackgroundResource(R.drawable.chip_green)
            view.setTextColor(0xFF1F4226.toInt())
        }
    }
}