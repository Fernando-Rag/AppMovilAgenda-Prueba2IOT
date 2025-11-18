package com.example.appmovilagenda

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.widget.CalendarView
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

class CalendarioActivity : BaseActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var btnMenu: ImageView

    private lateinit var chipMes: TextView
    private lateinit var chipSemana: TextView
    private lateinit var chipDia: TextView

    private lateinit var calendarView: CalendarView
    private lateinit var tvMes: TextView
    private lateinit var btnPrevMes: TextView
    private lateinit var btnNextMes: TextView

    private val localeCL = Locale("es", "CL")
    private val sdfDia = SimpleDateFormat("dd-MM-yyyy", localeCL)
    private val sdfMes = SimpleDateFormat("LLLL 'de' yyyy", localeCL)

    // Mes mostrado en el header (primer día del mes)
    private val calMes: Calendar = Calendar.getInstance(localeCL).apply {
        set(Calendar.DAY_OF_MONTH, 1)
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
                R.id.nav_calendario -> { /* ya aquí */ }
                R.id.nav_semana -> startActivity(Intent(this, SemanaActivity::class.java))
                R.id.nav_dia -> startActivity(Intent(this, DiaActivity::class.java))
            }
            drawerLayout.closeDrawers()
            true
        }
        setupDrawerHeaderClose()

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
        tvMes = findViewById(R.id.tvMes)
        btnPrevMes = findViewById(R.id.btnPrevMes)
        btnNextMes = findViewById(R.id.btnNextMes)

        // Inicializa header y mueve el calendario al mes del header
        actualizarHeaderMes()
        calendarView.date = calMes.timeInMillis

        btnPrevMes.setOnClickListener {
            calMes.add(Calendar.MONTH, -1)
            actualizarHeaderMes()
            calendarView.date = calMes.timeInMillis
        }
        btnNextMes.setOnClickListener {
            calMes.add(Calendar.MONTH, 1)
            actualizarHeaderMes()
            calendarView.date = calMes.timeInMillis
        }

        // Selección de día -> abre Día y sincroniza header si cambió el mes
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val c = Calendar.getInstance(localeCL).apply { set(year, month, dayOfMonth, 0, 0, 0) }
            if (year != calMes.get(Calendar.YEAR) || month != calMes.get(Calendar.MONTH)) {
                calMes.set(Calendar.YEAR, year)
                calMes.set(Calendar.MONTH, month)
                calMes.set(Calendar.DAY_OF_MONTH, 1)
                actualizarHeaderMes()
            }
            val fechaStr = sdfDia.format(c.time)
            startActivity(Intent(this, DiaActivity::class.java).putExtra(DiaActivity.EXTRA_FECHA, fechaStr))
        }

        // Sincronizar header cuando el usuario DESLIZA el CalendarView
        instalarSyncSwipeConHeader()
    }

    private fun instalarSyncSwipeConHeader() {
        // Umbral de desplazamiento para considerar que cambió de mes
        val thresholdPx = (80f * resources.displayMetrics.density)
        var startY = 0f
        var acumuladoY = 0f
        var tracking = false

        calendarView.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startY = ev.y
                    acumuladoY = 0f
                    tracking = true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (tracking) {
                        val dy = ev.y - startY
                        acumuladoY = dy
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (tracking) {
                        val dy = acumuladoY
                        if (abs(dy) > thresholdPx) {
                            // Nota: en CalendarView el scroll vertical suele ser:
                            // deslizar hacia arriba -> siguiente mes (incrementa)
                            // deslizar hacia abajo -> mes anterior (decrementa)
                            if (dy < 0) {
                                calMes.add(Calendar.MONTH, 1)
                            } else {
                                calMes.add(Calendar.MONTH, -1)
                            }
                            actualizarHeaderMes()
                            // Ajustamos la fecha del CalendarView para mantener ambas vistas sincronizadas
                            calendarView.date = calMes.timeInMillis
                        }
                    }
                    tracking = false
                    startY = 0f
                    acumuladoY = 0f
                }
            }
            // Devolvemos false para que el CalendarView siga recibiendo el gesto y haga su animación
            false
        }
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

    private fun actualizarHeaderMes() {
        val txt = sdfMes.format(calMes.time).replaceFirstChar { it.titlecase(localeCL) }
        tvMes.text = txt
    }
}