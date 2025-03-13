package de.peekandpoke.ultra.slumber.builtin.datetime.kotlinx

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer
import de.peekandpoke.ultra.slumber.builtin.datetime.TIMEZONE
import de.peekandpoke.ultra.slumber.builtin.datetime.TS
import de.peekandpoke.ultra.slumber.builtin.datetime.toMap
import de.peekandpoke.ultra.slumber.builtin.datetime.utc
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

object LocalDateAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): LocalDate? {

        if (data !is Map<*, *>) {
            return null
        }

        val timezone = (data[TIMEZONE] as? String?)?.let { tz -> TimeZone.of(tz) } ?: TimeZone.UTC

        return when (val ts = data[TS]) {
            is Number -> {
                val instant = Instant.fromEpochMilliseconds(ts.toLong())

                instant.toLocalDateTime(timezone).date
            }
            else -> null
        }
    }
}

object LocalDateSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any>? {

        if (data !is LocalDate) {
            return null
        }

        val zoned = data.atStartOfDayIn(TimeZone.UTC)

        return toMap(
            zoned.toEpochMilliseconds(),
            utc,
            zoned.toString()
        )
    }
}
