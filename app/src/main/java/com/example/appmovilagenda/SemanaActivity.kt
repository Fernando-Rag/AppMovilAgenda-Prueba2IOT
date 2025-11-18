package com.example.appmovilagenda

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SemanaActivity : BaseActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var btnMenu: ImageView

    private lateinit var chipMes: TextView
    private lateinit var chipSemana: TextView
    private lateinit var chipDia: TextView
    private lateinit var recyclerSemana: RecyclerView
    private lateinit var adapter: TareaAdapter

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private var tareasListener: ListenerRegistration? = null

    private val localeCL = Locale("es", "CL")
    private val formatoFecha = SimpleDateFormat("dd-MM-yyyy", localeCL)
    private val formatoHora = SimpleDateFormat("HH:mm", localeCL)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_semana_drawer)
        title = "Esta Semana"

        // Drawer
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        btnMenu = findViewById(R.id.btnMenu)
        btnMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_tareas -> startActivity(Intent(this, InicioTareasActivity::class.java))
                R.id.nav_recordatorios -> startActivity(Intent(this, RecordatoriosActivity::class.java))
                R.id.nav_calendario -> startActivity(Intent(this, CalendarioActivity::class.java))
                R.id.nav_semana -> { /* ya aquí */ }
                R.id.nav_dia -> startActivity(Intent(this, DiaActivity::class.java))
            }
            drawerLayout.closeDrawers()
            true
        }
        setupDrawerHeaderClose()

        // Chips
        chipMes = findViewById(R.id.chipMes)
        chipSemana = findViewById(R.id.chipSemana)
        chipDia = findViewById(R.id.chipDia)

        setChipSelected(chipMes, false)
        setChipSelected(chipSemana, true)
        setChipSelected(chipDia, false)

        chipMes.setOnClickListener { startActivity(Intent(this, CalendarioActivity::class.java)) }
        chipSemana.setOnClickListener { /* ya aquí */ }
        chipDia.setOnClickListener { startActivity(Intent(this, DiaActivity::class.java)) }

        recyclerSemana = findViewById(R.id.recyclerSemana)
        recyclerSemana.layoutManager = LinearLayoutManager(this)

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
        recyclerSemana.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Inicia sesión para ver tus tareas", Toast.LENGTH_SHORT).show()
            adapter.actualizarLista(emptyList())
            return
        }

        val (inicioSemana, finSemana) = obtenerRangoSemanaActual()

        tareasListener = db.collection("todos")
            .whereEqualTo("userId", uid)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error cargando: ${error.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                val todas = snapshot?.documents?.map { doc ->
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

                val deEstaSemana = todas.filter { tarea ->
                    val fechaMs = parseFechaMillis(tarea.fecha) ?: return@filter false
                    fechaMs in inicioSemana..finSemana
                }.sortedWith(compareBy(
                    { parseFechaMillis(it.fecha) ?: Long.MAX_VALUE },
                    { parseHoraMin(it.hora) }
                ))

                adapter.actualizarLista(deEstaSemana)
            }
    }

    override fun onStop() {
        super.onStop()
        tareasListener?.remove()
        tareasListener = null
    }

    private fun setupDrawerHeaderClose() {
        val header = if (navView.headerCount > 0) navView.getHeaderView(0)
        else navView.inflateHeaderView(R.layout.drawer_header)
        header.findViewById<ImageView>(R.id.btnMenuHeader)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
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

    private fun obtenerRangoSemanaActual(): Pair<Long, Long> {
        val cal = Calendar.getInstance(localeCL)
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val diaSemana = cal.get(Calendar.DAY_OF_WEEK)
        val offset = if (diaSemana == Calendar.SUNDAY) -6 else Calendar.MONDAY - diaSemana
        cal.add(Calendar.DAY_OF_MONTH, offset)
        val inicio = cal.timeInMillis

        cal.add(Calendar.DAY_OF_MONTH, 6)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val fin = cal.timeInMillis

        return inicio to fin
    }

    private fun parseFechaMillis(fecha: String): Long? {
        return try {
            val d = formatoFecha.parse(fecha) ?: return null
            val cal = Calendar.getInstance(localeCL).apply {
                time = d
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        } catch (_: Exception) {
            null
        }
    }

    private fun parseHoraMin(hora: String): Int {
        return try {
            if (hora.isBlank()) return 24 * 60
            val h = formatoHora.parse(hora) ?: return 24 * 60
            val cal = Calendar.getInstance(localeCL).apply { time = h }
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        } catch (_: Exception) {
            24 * 60
        }
    }
}