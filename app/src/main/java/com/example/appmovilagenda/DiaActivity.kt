package com.example.appmovilagenda

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiaActivity : BaseActivity() {

    companion object { const val EXTRA_FECHA = "extra_fecha_dia" }

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var btnMenu: ImageView

    private lateinit var chipMes: TextView
    private lateinit var chipSemana: TextView
    private lateinit var chipDia: TextView

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: TareaAdapter

    // Label de estado vacío (centrado)
    private var emptyView: TextView? = null

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private var listener: ListenerRegistration? = null

    private val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.forLanguageTag("es-CL"))
    private lateinit var fechaSeleccionada: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dia_drawer)
        title = "Día"

        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        btnMenu = findViewById(R.id.btnMenu)
        btnMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_tareas -> startActivity(Intent(this, InicioTareasActivity::class.java))
                R.id.nav_calendario -> startActivity(Intent(this, CalendarioActivity::class.java))
                R.id.nav_semana -> startActivity(Intent(this, SemanaActivity::class.java))
                R.id.nav_dia -> { /* ya aquí */ }
            }
            drawerLayout.closeDrawers()
            true
        }
        setupDrawerHeaderClose()

        fechaSeleccionada = intent.getStringExtra(EXTRA_FECHA) ?: sdf.format(Date())

        chipMes = findViewById(R.id.chipMes)
        chipSemana = findViewById(R.id.chipSemana)
        chipDia = findViewById(R.id.chipDia)

        setChipSelected(chipMes, false)
        setChipSelected(chipSemana, false)
        setChipSelected(chipDia, true)

        chipMes.setOnClickListener { startActivity(Intent(this, CalendarioActivity::class.java)) }
        chipSemana.setOnClickListener { startActivity(Intent(this, SemanaActivity::class.java)) }
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
            }
            startActivity(intent)
        }
        recycler.adapter = adapter

        // Inserta el label vacío centrado en el mismo contenedor del recycler
        attachEmptyLabel()
        showEmptyState(true, "Cargando tareas…")
    }

    override fun onStart() {
        super.onStart()
        val uid = auth.currentUser?.uid
        if (uid == null) {
            showEmptyState(true, "Inicia sesión para ver tus tareas")
            adapter.actualizarLista(emptyList())
            return
        }

        listener?.remove()
        listener = db.collection("todos")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    showEmptyState(true, "Error al cargar tareas")
                    adapter.actualizarLista(emptyList())
                    return@addSnapshotListener
                }
                val delDia = snapshot?.documents?.map { doc ->
                    Tarea(
                        id = doc.id,
                        titulo = doc.getString("titulo").orEmpty(),
                        descripcion = doc.getString("descripcion").orEmpty(),
                        fecha = doc.getString("fecha").orEmpty(),
                        hora = doc.getString("hora").orEmpty(),
                        createdAt = doc.getTimestamp("createdAt"),
                        userId = doc.getString("userId").orEmpty()
                    )
                }.orEmpty()
                    .filter { it.fecha == fechaSeleccionada }
                    .sortedWith(compareBy({ parseHoraMin(it.hora) }, { it.titulo }))

                adapter.actualizarLista(delDia)
                if (delDia.isEmpty()) {
                    showEmptyState(true, "No hay registro de ninguna tarea para este día.\nAgrega una con el botón +")
                } else {
                    showEmptyState(false)
                }
            }
    }

    override fun onStop() {
        super.onStop()
        listener?.remove(); listener = null
    }

    private fun setupDrawerHeaderClose() {
        val header = if (navView.headerCount > 0) navView.getHeaderView(0)
        else navView.inflateHeaderView(R.layout.drawer_header)
        header.findViewById<ImageView>(R.id.btnMenuHeader)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun setChipSelected(view: TextView, selected: Boolean) {
        if (selected) { view.setBackgroundResource(R.drawable.chip_brown); view.setTextColor(0xFF6B4E2E.toInt()) }
        else { view.setBackgroundResource(R.drawable.chip_green); view.setTextColor(0xFF1F4226.toInt()) }
    }

    private fun parseHoraMin(hora: String): Int = try {
        if (hora.isBlank()) 24 * 60 else {
            val (h,m) = hora.split(":").map { it.toInt() }
            h*60 + m
        }
    } catch (_: Exception) { 24*60 }

    // ---- Estado vacío centrado ----

    private fun attachEmptyLabel() {
        val parent = recycler.parent as? ViewGroup ?: return

        // Evita duplicarlo si ya existe
        emptyView = parent.findViewWithTag("empty_day_label") as? TextView
        if (emptyView != null) return

        val label = TextView(this).apply {
            tag = "empty_day_label"
            text = "No hay registro de ninguna tarea para este día.\nAgrega una con el botón +"
            setTextColor(0xFF9AA0A6.toInt()) // gris
            textSize = 16f
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            visibility = View.GONE
        }

        val lp: ViewGroup.LayoutParams = when (parent) {
            is ConstraintLayout -> {
                if (parent.id == View.NO_ID) parent.id = View.generateViewId()
                ConstraintLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                }
            }
            is LinearLayout -> {
                val params = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.gravity = Gravity.CENTER
                params
            }
            else -> {
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }

        parent.addView(label, lp)
        emptyView = label
    }

    private fun showEmptyState(show: Boolean, message: String? = null) {
        emptyView?.let { tv ->
            if (message != null) tv.text = message
            tv.visibility = if (show) View.VISIBLE else View.GONE
        }
        recycler.visibility = if (show) View.GONE else View.VISIBLE
    }
}