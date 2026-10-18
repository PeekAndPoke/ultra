package io.peekandpoke.ultra.slumber.builtin.datetime.javatime

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer
import java.time.LocalTime

/** Awaker for [LocalTime] values. Reads a seconds-of-day [Int] (`0..86399`); anything outside that range awakes to null. */
object LocalTimeAwaker : Awaker {

    private val validRange = (0L..86399L)

    override fun awake(data: Any?, context: Awaker.Context): LocalTime? {

        if (data !is Number) {
            return null
        }

        return when (val num = data.toLong()) {
            in validRange -> LocalTime.ofSecondOfDay(num)
            else -> null
        }
    }
}

/** Slumberer for [LocalTime] values. Writes the seconds-of-day as an [Int] (`0..86399`), no wrapper map. */
object LocalTimeSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Int? {

        if (data !is LocalTime) {
            return null
        }

        return data.toSecondOfDay()
    }
}
