package com.example.appmovilagenda

import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent
import android.widget.ImageView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class RecordatoriosActivity : BaseActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var btnMenu: ImageView

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ReminderAdapter

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recordatorios_drawer)
        title = "Recordatorios"

        // Drawer
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        btnMenu = findViewById(R.id.btnMenu)
        btnMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_tareas -> startActivity(Intent(this, InicioTareasActivity::class.java))
                R.id.nav_recordatorios -> { /* ya aquí */ }
                R.id.nav_calendario -> startActivity(Intent(this, CalendarioActivity::class.java))
                R.id.nav_semana -> startActivity(Intent(this, SemanaActivity::class.java))
                R.id.nav_dia -> startActivity(Intent(this, DiaActivity::class.java))
            }
            drawerLayout.closeDrawers()
            true
        }
        setupDrawerHeaderClose()

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

    private fun setupDrawerHeaderClose() {
        val header = if (navView.headerCount > 0) navView.getHeaderView(0)
        else navView.inflateHeaderView(R.layout.drawer_header)
        header.findViewById<ImageView>(R.id.btnMenuHeader)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }
}