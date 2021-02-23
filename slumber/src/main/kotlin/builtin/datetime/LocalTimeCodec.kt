package de.peekandpoke.ultra.slumber.builtin.datetime

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer
import java.time.LocalTime

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

object LocalTimeSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Int? {

        if (data !is LocalTime) {
            return null
        }

        return data.toSecondOfDay()
    }
}
