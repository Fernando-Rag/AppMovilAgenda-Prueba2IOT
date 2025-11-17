package com.example.appmovilagenda

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class InicioTareasActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tareaAdapter: TareaAdapter

    // Drawer
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var btnMenu: ImageView

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private var tareasListener: ListenerRegistration? = null

    private val crearTareaLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            val data = result.data ?: return@registerForActivityResult

            val titulo = data.getStringExtra(CrearTareaActivity.EXTRA_TITULO).orEmpty()
            val descripcion = data.getStringExtra(CrearTareaActivity.EXTRA_DESCRIPCION).orEmpty()
            val fecha = data.getStringExtra(CrearTareaActivity.EXTRA_FECHA).orEmpty()
            val hora = data.getStringExtra(CrearTareaActivity.EXTRA_HORA).orEmpty()
            val recordatorioMillis = data.getLongExtra(CrearTareaActivity.EXTRA_RECORDATORIO_MILLIS, -1L)

            if (titulo.isBlank()) {
                Toast.makeText(this, "El título es obligatorio", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            val uid = auth.currentUser?.uid
            if (uid == null) {
                Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            val doc = hashMapOf(
                "titulo" to titulo,
                "descripcion" to descripcion,
                "fecha" to fecha,
                "hora" to hora,
                "recordatorioMillis" to if (recordatorioMillis > 0) recordatorioMillis else null,
                "userId" to uid,
                "createdAt" to FieldValue.serverTimestamp()
            )

            db.collection("todos")
                .add(doc)
                .addOnSuccessListener { ref ->
                    if (recordatorioMillis > System.currentTimeMillis()) {
                        programarRecordatorio(ref.id, titulo, recordatorioMillis)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Usamos el contenedor con Drawer (no se reemplaza tu diseño, solo se envuelve)
        setContentView(R.layout.activity_inicio_tareas_drawer)

        NotificationHelper.createChannel(this)
        pedirPermisoNotificaciones()

        // Drawer
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        btnMenu = findViewById(R.id.btnMenu)
        btnMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        // Clics del menú
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_tareas -> { /* ya aquí */ }
                R.id.nav_recordatorios -> startActivity(Intent(this, RecordatoriosActivity::class.java))
                R.id.nav_calendario -> startActivity(Intent(this, CalendarioActivity::class.java))
                R.id.nav_semana -> startActivity(Intent(this, SemanaActivity::class.java))
                R.id.nav_dia -> startActivity(Intent(this, DiaActivity::class.java))
            }
            drawerLayout.closeDrawers()
            true
        }

        // Habilitar el botón de hamburguesa del header del Drawer para cerrar el panel
        setupDrawerHeaderClose()

        // Lista
        recyclerView = findViewById(R.id.recyclerTareas)
        // Activa cuadrícula de 2 columnas para ver tarjetas como en el mockup:
        // recyclerView.layoutManager = GridLayoutManager(this, 2)
        // O deja lista vertical:
        recyclerView.layoutManager = LinearLayoutManager(this)

        tareaAdapter = TareaAdapter(emptyList()) { tarea ->
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
        recyclerView.adapter = tareaAdapter

        // Swipe con confirmación
        val swipe = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val position = vh.bindingAdapterPosition
                val tarea = tareaAdapter.getItemAt(position)
                recyclerView.adapter?.notifyItemChanged(position)
                if (tarea.id.isBlank()) return
                MaterialAlertDialogBuilder(this@InicioTareasActivity)
                    .setTitle("Eliminar tarea")
                    .setMessage("¿Seguro que deseas eliminar \"${tarea.titulo}\"?")
                    .setNegativeButton("Cancelar") { d, _ -> d.dismiss() }
                    .setPositiveButton("Eliminar") { d, _ ->
                        d.dismiss()
                        eliminarTareaConUndo(tarea)
                    }
                    .show()
            }
        }
        ItemTouchHelper(swipe).attachToRecyclerView(recyclerView)

        val fab: FloatingActionButton = findViewById(R.id.btnAgregar)
        fab.setOnClickListener {
            val intent = Intent(this, CrearTareaActivity::class.java)
            crearTareaLauncher.launch(intent)
        }

        // Botón calendario del header
        findViewById<ImageView>(R.id.btnCalendario)?.setOnClickListener {
            startActivity(Intent(this, CalendarioActivity::class.java))
        }
    }

    // Toma el header del NavigationView y asigna el click para cerrar el Drawer
    private fun setupDrawerHeaderClose() {
        // Si el header ya está, úsalo; si no, infla el layout del header
        val header = if (navView.headerCount > 0) navView.getHeaderView(0)
        else navView.inflateHeaderView(R.layout.drawer_header)

        header.findViewById<ImageView>(R.id.btnMenuHeader)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    override fun onStart() {
        super.onStart()
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Inicia sesión para ver tus tareas", Toast.LENGTH_SHORT).show()
            tareaAdapter.actualizarLista(emptyList())
            return
        }

        tareasListener = db.collection("todos")
            .whereEqualTo("userId", uid)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error cargando: ${error.message}", Toast.LENGTH_LONG).show()
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
                tareaAdapter.actualizarLista(tareas)
            }
    }

    override fun onStop() {
        super.onStop()
        tareasListener?.remove()
        tareasListener = null
    }

    private fun eliminarTareaConUndo(tarea: Tarea) {
        val ref = db.collection("todos").document(tarea.id)
        ref.delete()
            .addOnSuccessListener {
                // Cancelar recordatorio si existía
                cancelarRecordatorio(tarea.id)

                Snackbar.make(recyclerView, "Tarea eliminada", Snackbar.LENGTH_LONG)
                    .setAction("Deshacer") {
                        val uid = auth.currentUser?.uid ?: return@setAction
                        val data = mapOf(
                            "titulo" to tarea.titulo,
                            "descripcion" to tarea.descripcion,
                            "fecha" to tarea.fecha,
                            "hora" to tarea.hora,
                            "recordatorioMillis" to tarea.recordatorioMillis,
                            "userId" to uid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                        // Recrea el doc con el mismo id
                        db.collection("todos").document(tarea.id).set(data).addOnSuccessListener {
                            tarea.recordatorioMillis?.let { millis ->
                                if (millis > System.currentTimeMillis()) {
                                    programarRecordatorio(tarea.id, tarea.titulo, millis)
                                }
                            }
                        }
                    }
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "No se pudo eliminar", Toast.LENGTH_SHORT).show()
            }
    }

    private fun programarRecordatorio(tareaId: String, titulo: String, millis: Long) {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, RecordatorioReceiver::class.java).apply {
            putExtra("tareaId", tareaId)
            putExtra("titulo", titulo)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        val pi = PendingIntent.getBroadcast(this, tareaId.hashCode(), intent, flags)
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
    }

    private fun cancelarRecordatorio(tareaId: String) {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, RecordatorioReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        val pi = PendingIntent.getBroadcast(this, tareaId.hashCode(), intent, flags)
        am.cancel(pi)
    }

    private fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }
}