package com.example.appmovilagenda

import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar

class RecordatoriosActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ReminderAdapter

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private var listener: ListenerRegistration? = null

    private val sdfRec = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recordatorios)
        title = "Recordatorios"

        recycler = findViewById(R.id.recyclerRecordatorios)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = ReminderAdapter(emptyList())
        recycler.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Inicia sesión para ver recordatorios", Toast.LENGTH_SHORT).show()
            adapter.update(emptyList())
            return
        }

        // Solo tareas con recordatorioMillis válido, ordenadas por el recordatorio
        listener = db.collection("todos")
            .whereEqualTo("userId", uid)
            .whereGreaterThan("recordatorioMillis", 0)
            .orderBy("recordatorioMillis", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Toast.makeText(this, "Error: ${err.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                val list = snap?.documents?.map { d ->
                    Tarea(
                        id = d.id,
                        titulo = d.getString("titulo").orEmpty(),
                        descripcion = d.getString("descripcion").orEmpty(),
                        fecha = d.getString("fecha").orEmpty(),
                        hora = d.getString("hora").orEmpty(),
                        recordatorioMillis = d.getLong("recordatorioMillis"),
                        createdAt = d.getTimestamp("createdAt"),
                        userId = d.getString("userId").orEmpty()
                    )
                }.orEmpty()
                adapter.update(list)
            }
    }

    override fun onStop() {
        super.onStop()
        listener?.remove()
        listener = null
    }
}