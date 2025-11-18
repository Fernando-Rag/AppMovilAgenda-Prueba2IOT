package com.example.appmovilagenda

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.CalendarView

/**
 * CalendarView que bloquea el gesto de desplazamiento (swipe) para cambiar de mes,
 * pero permite taps para seleccionar días.
 */
class NoSwipeCalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.calendarViewStyle
) : CalendarView(context, attrs, defStyleAttr) {

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Intercepta SOLO el movimiento para bloquear el swipe
        return ev.actionMasked == MotionEvent.ACTION_MOVE
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Evitar que el padre (ScrollView) intercepte; dejamos pasar DOWN
                parent?.requestDisallowInterceptTouchEvent(true)
                return super.onTouchEvent(event)
            }
            MotionEvent.ACTION_MOVE -> {
                // Consumimos MOVE para que no cambie de mes por gesto
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Dejamos pasar UP/CANCEL para que funcione el tap en días
                return super.onTouchEvent(event)
            }
            else -> return super.onTouchEvent(event)
        }
    }
}