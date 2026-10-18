package io.peekandpoke.ultra.datetime

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

/**
 * Zone math for [MpTimezone], with java.time semantics on every target.
 *
 * Only "offset at an instant" is platform specific ([platformOffsetSecondsAt]). Everything else is derived here
 * from it plus UTC-only kotlinx calls, so no target depends on kotlinx-datetime's own time zone database.
 */
internal object TimezoneEngine {

    private val probeWindow = 1.days

    /** The offset of [timezone] at [instant], in seconds. */
    fun offsetSecondsAt(timezone: MpTimezone, instant: Instant): Int {
        return fixedOffsetSecondsOrNull(timezone.id) ?: platformOffsetSecondsAt(timezone.id, instant)
    }

    /** The wall-clock time of [instant] in [timezone]. */
    fun toLocalDateTime(timezone: MpTimezone, instant: Instant): LocalDateTime {
        return instant.plus(offsetSecondsAt(timezone, instant).seconds).toLocalDateTime(TimeZone.UTC)
    }

    /**
     * The instant of the wall-clock time [local] in [timezone], like java.time's `ZonedDateTime.ofLocal`.
     *
     * In a gap the result moves forward by the length of the gap. In an overlap [preferredOffsetSeconds] wins when
     * it is one of the two valid offsets, otherwise the earlier offset does.
     */
    fun toInstant(timezone: MpTimezone, local: LocalDateTime, preferredOffsetSeconds: Int? = null): Instant {
        val wall = local.toInstant(TimeZone.UTC)

        fixedOffsetSecondsOrNull(timezone.id)?.let { return wall - it.seconds }

        val resolution = resolve(timezone.id, wall)

        val offset = when (resolution) {
            is Resolution.Unique -> resolution.offset
            is Resolution.Overlap -> when (preferredOffsetSeconds) {
                resolution.later -> resolution.later
                else -> resolution.earlier
            }
            // Using the offset from before the transition shifts the wall time forward by the gap length.
            is Resolution.Gap -> resolution.before
        }

        return wall - offset.seconds
    }

    /** The first instant of [date] in [timezone]: midnight, or the end of the gap when midnight does not exist. */
    fun startOfDay(timezone: MpTimezone, date: LocalDate): Instant {
        val wall = date.atStartOfDayIn(TimeZone.UTC)

        fixedOffsetSecondsOrNull(timezone.id)?.let { return wall - it.seconds }

        return when (val resolution = resolve(timezone.id, wall)) {
            is Resolution.Unique -> wall - resolution.offset.seconds
            is Resolution.Overlap -> wall - resolution.earlier.seconds
            is Resolution.Gap -> transitionAfter(timezone.id, wall - probeWindow, resolution.before)
        }
    }

    /**
     * Adds [value] times [unit] to [instant] in [timezone], like java.time's `ZonedDateTime.plus`.
     *
     * Time-based units are added on the time line. Date-based units are added to the wall-clock date, keeping the
     * time of day and, in an overlap, the current offset.
     */
    fun plus(instant: Instant, value: Int, unit: DateTimeUnit, timezone: MpTimezone): Instant {
        return when (unit) {
            is DateTimeUnit.TimeBased -> instant.plus(value, unit)

            is DateTimeUnit.DateBased -> {
                val offset = offsetSecondsAt(timezone, instant)
                val local = instant.plus(offset.seconds).toLocalDateTime(TimeZone.UTC)
                val shifted = LocalDateTime(local.date.plus(value, unit), local.time)

                toInstant(timezone, shifted, preferredOffsetSeconds = offset)
            }
        }
    }

    private sealed interface Resolution {
        data class Unique(val offset: Int) : Resolution

        /** [earlier] is the offset before the transition, i.e. the one giving the earlier instant. */
        data class Overlap(val earlier: Int, val later: Int) : Resolution

        data class Gap(val before: Int) : Resolution
    }

    /**
     * Resolves the wall-clock time [wall] (as if it were UTC) in the zone [zoneId].
     *
     * Assumes at most one transition within [probeWindow] of [wall], which holds for every zone in the IANA database.
     */
    private fun resolve(zoneId: String, wall: Instant): Resolution {
        val before = platformOffsetSecondsAt(zoneId, wall - probeWindow)
        val after = platformOffsetSecondsAt(zoneId, wall + probeWindow)

        fun isValid(offset: Int) = platformOffsetSecondsAt(zoneId, wall - offset.seconds) == offset

        val validBefore = isValid(before)

        if (before == after) {
            return Resolution.Unique(if (validBefore) before else platformOffsetSecondsAt(zoneId, wall))
        }

        val validAfter = isValid(after)

        return when {
            validBefore && validAfter -> Resolution.Overlap(earlier = before, later = after)
            validBefore -> Resolution.Unique(before)
            validAfter -> Resolution.Unique(after)
            else -> Resolution.Gap(before = before)
        }
    }

    /** The first instant after [from] at which [zoneId] stops using the offset [before], to the second. */
    private fun transitionAfter(zoneId: String, from: Instant, before: Int): Instant {
        var lo = from.epochSeconds
        var hi = lo + 2 * probeWindow.inWholeSeconds

        while (hi - lo > 1) {
            val mid = lo + (hi - lo) / 2

            if (platformOffsetSecondsAt(zoneId, Instant.fromEpochSeconds(mid)) == before) lo = mid else hi = mid
        }

        return Instant.fromEpochSeconds(hi)
    }

    /**
     * The offset of a fixed-offset zone id, or null for a region id.
     *
     * Accepts what java.time's `ZoneId.of` accepts for fixed offsets: `Z`, `UTC`, `GMT`, `UT`, a bare offset
     * (`+h`, `+hh`, `+hh:mm`, `+hhmm`, `+hh:mm:ss`, `+hhmmss`) and those offsets after a `UTC`, `GMT` or `UT` prefix.
     */
    internal fun fixedOffsetSecondsOrNull(zoneId: String): Int? {
        val offset = when {
            zoneId == "Z" -> return 0
            zoneId.startsWith("+") || zoneId.startsWith("-") -> zoneId
            else -> {
                val prefix = listOf("UTC", "GMT", "UT").firstOrNull { zoneId.startsWith(it) } ?: return null
                val rest = zoneId.removePrefix(prefix).ifEmpty { return 0 }
                // e.g. "GMT0" is a region id, not an offset
                if (!rest.startsWith("+") && !rest.startsWith("-")) return null
                rest
            }
        }

        return parseOffsetSeconds(offset) ?: throw IllegalTimeZoneException("Invalid zone id: $zoneId")
    }

    private val offsetPattern = Regex("^([+-])(\\d{1,2})(?::?(\\d{2})(?::?(\\d{2}))?)?$")

    private fun parseOffsetSeconds(text: String): Int? {
        val match = offsetPattern.matchEntire(text) ?: return null
        val (sign, h, m, s) = match.destructured

        // java.time: one-digit hours only on their own, and the separator style must be consistent.
        if (h.length == 1 && m.isNotEmpty()) return null
        if (m.isNotEmpty() && text.contains(':') != (text.length == 6 || text.length == 9)) return null

        val hours = h.toInt()
        val minutes = m.ifEmpty { "0" }.toInt()
        val seconds = s.ifEmpty { "0" }.toInt()

        if (hours > 18 || minutes > 59 || seconds > 59) return null

        val total = hours * 3600 + minutes * 60 + seconds
        if (total > 18 * 3600) return null

        return if (sign == "-") -total else total
    }
}

/** The offset of the region zone [zoneId] at [instant], in seconds. Throws [IllegalTimeZoneException] if unknown. */
internal expect fun platformOffsetSecondsAt(zoneId: String, instant: Instant): Int

/** The id of the system's time zone. */
internal expect fun platformSystemDefaultZoneId(): String
