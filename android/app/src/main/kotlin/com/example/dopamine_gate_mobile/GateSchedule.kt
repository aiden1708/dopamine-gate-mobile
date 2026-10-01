package com.example.dopamine_gate_mobile

data class TimeWindow(val startMinutes: Int, val endMinutes: Int) {

    fun contains(currentMinutes: Int): Boolean {
        return if (startMinutes < endMinutes) {
            // Normal window, e.g. 06:00 → 07:00
            currentMinutes >= startMinutes && currentMinutes < endMinutes
        } else {
            // Crosses midnight, e.g. 22:00 → 00:00
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }
}

object GateSchedule {

    val allowedWindows =
            listOf(
                    TimeWindow(startMinutes = 22 * 60, endMinutes = 24 * 60),
                    TimeWindow(startMinutes = 6 * 60, endMinutes = 7 * 60)
                    // TimeWindow(startMinutes = 1 * 60, endMinutes = 2 * 60)
            )
}
