package com.example.appmovilagenda

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
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

// AHORA hereda de BaseActivity para poder usar performLogout()
class InicioTareasActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tareaAdapter: TareaAdapter

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
                "userId" to uid,
                "createdAt" to FieldValue.serverTimestamp()
            )

            db.collection("todos")
                .add(doc)
                .addOnSuccessListener { }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio_tareas_drawer)

        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        btnMenu = findViewById(R.id.btnMenu)
        btnMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_tareas -> {}
                R.id.nav_calendario -> startActivity(Intent(this, CalendarioActivity::class.java))
                R.id.nav_semana -> startActivity(Intent(this, SemanaActivity::class.java))
                R.id.nav_dia -> startActivity(Intent(this, DiaActivity::class.java))
            }
            drawerLayout.closeDrawers()
            true
        }

        setupDrawerViews()

        recyclerView = findViewById(R.id.recyclerTareas)
        recyclerView.layoutManager = LinearLayoutManager(this)

        tareaAdapter = TareaAdapter(emptyList()) { tarea ->
            val intent = Intent(this, EditarTareaActivity::class.java).apply {
                putExtra(EditarTareaActivity.EXTRA_ID, tarea.id)
                putExtra(EditarTareaActivity.EXTRA_TITULO, tarea.titulo)
                putExtra(EditarTareaActivity.EXTRA_DESCRIPCION, tarea.descripcion)
                putExtra(EditarTareaActivity.EXTRA_FECHA, tarea.fecha)
                putExtra(EditarTareaActivity.EXTRA_HORA, tarea.hora)
            }
            startActivity(intent)
        }
        recyclerView.adapter = tareaAdapter

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

        findViewById<FloatingActionButton>(R.id.btnAgregar).setOnClickListener {
            crearTareaLauncher.launch(Intent(this, CrearTareaActivity::class.java))
        }

        findViewById<ImageView>(R.id.btnCalendario)?.setOnClickListener {
            startActivity(Intent(this, CalendarioActivity::class.java))
        }
    }

    private fun setupDrawerViews() {
        // Header: botón para cerrar el drawer
        val header = if (navView.headerCount > 0) navView.getHeaderView(0)
        else navView.inflateHeaderView(R.layout.drawer_header)
        header.findViewById<ImageView>(R.id.btnMenuHeader)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        // Footer: botón Cerrar sesión en la zona inferior
        findViewById<View>(R.id.btnLogoutFooter)?.setOnClickListener {
            performLogout()
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
                Snackbar.make(recyclerView, "Tarea eliminada", Snackbar.LENGTH_LONG)
                    .setAction("Deshacer") {
                        val uid = auth.currentUser?.uid ?: return@setAction
                        val data = mapOf(
                            "titulo" to tarea.titulo,
                            "descripcion" to tarea.descripcion,
                            "fecha" to tarea.fecha,
                            "hora" to tarea.hora,
                            "userId" to uid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                        db.collection("todos").document(tarea.id)
                            .set(data)
                            .addOnSuccessListener { }
                    }
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "No se pudo eliminar", Toast.LENGTH_SHORT).show()
            }
    }
}