package de.peekandpoke.ultra.common.datetime

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

object MpDateTimeParser {

    @Suppress("RegExpRedundantEscape")
    private val isoWithTimezone = "^([0-9-]+)T([0-9:\\.]+)(Z|\\[.+\\])?$".toRegex()

    fun parseInstant(isoString: String): MpInstant {

        val (datetime, timezone) = parse(isoString)

        return datetime.toInstant(timezone)
    }

    fun parseZonedDateTime(isoString: String): MpZonedDateTime {

        val (datetime, timezone) = parse(isoString)

        return datetime.atZone(timezone)
    }

    private fun parse(isoString: String): Pair<MpLocalDateTime, TimeZone> {
        val match = isoWithTimezone.find(isoString)
        // TODO: use more specific
            ?: throw RuntimeException("Cannot parse iso date string '$isoString'")

        val date = match.groupValues[1]
        val time = match.groupValues[2]
        val tz = match.groupValues[3]

        val timezone = when (tz) {
            "", "Z" -> TimeZone.UTC
            else -> TimeZone.of(tz.drop(1).dropLast(1))
        }

        val localDateTime = MpLocalDateTime(LocalDateTime.parse("${date}T$time"))

        return Pair(localDateTime, timezone)
    }
}
