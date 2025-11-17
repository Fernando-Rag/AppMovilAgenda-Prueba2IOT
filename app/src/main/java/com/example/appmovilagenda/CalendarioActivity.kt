package com.example.appmovilagenda

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class CalendarioActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var btnMenu: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Usar el contenedor con Drawer para esta pantalla
        setContentView(R.layout.activity_calendario_drawer)

        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        btnMenu = findViewById(R.id.btnMenu)

        // Abrir Drawer con la hamburguesa (en la pantalla)
        btnMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        // Navegación del Drawer (mismo menú que Inicio)
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_tareas -> startActivity(Intent(this, InicioTareasActivity::class.java))
                R.id.nav_recordatorios -> startActivity(Intent(this, RecordatoriosActivity::class.java))
                R.id.nav_calendario -> { /* ya aquí */ }
                R.id.nav_semana -> startActivity(Intent(this, SemanaActivity::class.java))
                R.id.nav_dia -> startActivity(Intent(this, DiaActivity::class.java))
            }
            drawerLayout.closeDrawers()
            true
        }

        // Habilita que el ícono de hamburguesa del header del Drawer cierre el panel
        setupDrawerHeaderClose()

        // TODO: lógica de cambiar mes, y chips para navegar:
        // findViewById<TextView>(R.id.chipSemana).setOnClickListener { startActivity(Intent(this, SemanaActivity::class.java)) }
        // findViewById<TextView>(R.id.chipDia).setOnClickListener { startActivity(Intent(this, DiaActivity::class.java)) }
    }

    // Toma el header del NavigationView y asigna el click para cerrar el Drawer
    private fun setupDrawerHeaderClose() {
        // Si el header ya está inflado, úsalo; si no, inflar el layout del header
        val header = if (navView.headerCount > 0) navView.getHeaderView(0)
        else navView.inflateHeaderView(R.layout.drawer_header)

        header.findViewById<ImageView>(R.id.btnMenuHeader)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }
}