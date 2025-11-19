package com.example.appmovilagenda

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.CalendarView
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarioActivity : BaseActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var btnMenu: ImageView

    private lateinit var chipMes: TextView
    private lateinit var chipSemana: TextView
    private lateinit var chipDia: TextView

    private lateinit var calendarView: CalendarView

    private val localeCL = Locale("es", "CL")
    private val sdfDia = SimpleDateFormat("dd-MM-yyyy", localeCL)

    private val calMes: Calendar = Calendar.getInstance(localeCL).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendario_drawer)

        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        btnMenu = findViewById(R.id.btnMenu)
        btnMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_tareas -> startActivity(Intent(this, InicioTareasActivity::class.java))
                R.id.nav_calendario -> { /* actual */ }
                R.id.nav_semana -> startActivity(Intent(this, SemanaActivity::class.java))
                R.id.nav_dia -> startActivity(Intent(this, DiaActivity::class.java))
            }
            drawerLayout.closeDrawers()
            true
        }
        setupDrawerViews()

        chipMes = findViewById(R.id.chipMes)
        chipSemana = findViewById(R.id.chipSemana)
        chipDia = findViewById(R.id.chipDia)
        setChipSelected(chipMes, true)
        setChipSelected(chipSemana, false)
        setChipSelected(chipDia, false)
        chipMes.setOnClickListener { /* ya aquí */ }
        chipSemana.setOnClickListener { startActivity(Intent(this, SemanaActivity::class.java)) }
        chipDia.setOnClickListener { startActivity(Intent(this, DiaActivity::class.java)) }

        calendarView = findViewById(R.id.calendarView)
        calendarView.date = calMes.timeInMillis

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val c = Calendar.getInstance(localeCL).apply { set(year, month, dayOfMonth, 0, 0, 0) }
            val fechaStr = sdfDia.format(c.time)
            startActivity(Intent(this, DiaActivity::class.java).putExtra(DiaActivity.EXTRA_FECHA, fechaStr))
        }

        bloquearSwipePeroPermitirTap()
    }

    private fun bloquearSwipePeroPermitirTap() {
        calendarView.setOnTouchListener { v, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    false
                }
                MotionEvent.ACTION_MOVE -> true
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                    false
                }
                else -> false
            }
        }
    }

    private fun setupDrawerViews() {
        val header = if (navView.headerCount > 0) navView.getHeaderView(0)
        else navView.inflateHeaderView(R.layout.drawer_header)
        header.findViewById<ImageView>(R.id.btnMenuHeader)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        findViewById<View>(R.id.btnLogoutFooter)?.setOnClickListener { performLogout() }
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