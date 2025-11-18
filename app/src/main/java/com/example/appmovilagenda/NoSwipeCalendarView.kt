package com.example.appmovilagenda

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.CalendarView

/**
 * Bloquea el swipe (cambio de mes por gesto) y permite taps para seleccionar días.
 */
class NoSwipeCalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.calendarViewStyle
) : CalendarView(context, attrs, defStyleAttr) {

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // No interceptes DOWN/UP para que el tap llegue al manejador interno.
        // Solo deja que onTouchEvent consuma los MOVE.
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Evita que el ScrollView padre intercepte este gesto.
                parent?.requestDisallowInterceptTouchEvent(true)
                // Deja que CalendarView maneje el DOWN (necesario para seleccionar).
                return super.onTouchEvent(event)
            }
            MotionEvent.ACTION_MOVE -> {
                // Mantén bloqueado al padre y consume el MOVE para impedir el swipe de mes.
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Permite que CalendarView procese la selección al levantar el dedo.
                parent?.requestDisallowInterceptTouchEvent(false)
                return super.onTouchEvent(event)
            }
            else -> return super.onTouchEvent(event)
        }
    }
}