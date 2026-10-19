package io.peekandpoke.ultra.datetime

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.floor
import kotlin.time.Instant

/**
 * Epoch seconds ↔ wall-clock date and time in `Int` / `Double` arithmetic (Hinnant's civil-from-days and back).
 *
 * kotlinx-datetime's own conversion is `Long`-based, and `Long` is emulated, i.e. slow, on Kotlin/JS. Exact while the
 * day count fits an `Int` and the seconds a `Double`'s 53 bits — about ±1 million years here; beyond that kotlinx does
 * the conversion.
 */
internal object EpochMath {

    private const val SECONDS_PER_DAY = 86_400.0
    private const val MAX_EXACT_YEAR = 1_000_000
    private const val MAX_EXACT_SECONDS = MAX_EXACT_YEAR * 366.0 * SECONDS_PER_DAY

    /** The date and time at [epochSeconds] (wall-clock seconds, i.e. already shifted by an offset) plus [nanos]. */
    fun localDateTime(epochSeconds: Double, nanos: Int): LocalDateTime {
        if (abs(epochSeconds) > MAX_EXACT_SECONDS) {
            return Instant.fromEpochSeconds(epochSeconds.toLong(), nanos).toLocalDateTime(TimeZone.UTC)
        }

        val days = floor(epochSeconds / SECONDS_PER_DAY)
        val secondOfDay = (epochSeconds - days * SECONDS_PER_DAY).toInt()

        // civil from days, http://howardhinnant.github.io/date_algorithms.html
        val z = days.toInt() + 719_468
        val era = (if (z >= 0) z else z - 146_096) / 146_097
        val dayOfEra = z - era * 146_097
        val yearOfEra = (dayOfEra - dayOfEra / 1_460 + dayOfEra / 36_524 - dayOfEra / 146_096) / 365
        val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
        val mp = (5 * dayOfYear + 2) / 153
        val day = dayOfYear - (153 * mp + 2) / 5 + 1
        val month = if (mp < 10) mp + 3 else mp - 9
        val year = yearOfEra + era * 400 + if (month <= 2) 1 else 0

        return LocalDateTime(
            year = year,
            month = month,
            day = day,
            hour = secondOfDay / 3_600,
            minute = secondOfDay / 60 % 60,
            second = secondOfDay % 60,
            nanosecond = nanos,
        )
    }

    /** The epoch seconds of [local] read as UTC (whole seconds; the nanoseconds are left out). */
    fun epochSeconds(local: LocalDateTime): Double {
        if (abs(local.year) > MAX_EXACT_YEAR) {
            return local.toInstant(TimeZone.UTC).epochSeconds.toDouble()
        }

        return epochDays(local.year, local.month.number, local.day) * SECONDS_PER_DAY +
                local.hour * 3_600 + local.minute * 60 + local.second
    }

    /** The epoch seconds of the start of [date] read as UTC. */
    fun epochSeconds(date: LocalDate): Double {
        if (abs(date.year) > MAX_EXACT_YEAR) {
            return date.atStartOfDayIn(TimeZone.UTC).epochSeconds.toDouble()
        }

        return epochDays(date.year, date.month.number, date.day) * SECONDS_PER_DAY
    }

    /** The instant at [epochSeconds] (whole seconds) plus [nanos]. */
    fun instant(epochSeconds: Double, nanos: Int): Instant {
        return Instant.fromEpochSeconds(epochSeconds.toLong(), nanos)
    }

    // days from civil, http://howardhinnant.github.io/date_algorithms.html
    private fun epochDays(year: Int, month: Int, day: Int): Double {
        val y = if (month <= 2) year - 1 else year
        val era = (if (y >= 0) y else y - 399) / 400
        val yearOfEra = y - era * 400
        val dayOfYear = (153 * (if (month > 2) month - 3 else month + 9) + 2) / 5 + day - 1
        val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear

        return era * 146_097.0 + dayOfEra - 719_468
    }
}
