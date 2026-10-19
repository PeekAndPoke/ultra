package io.peekandpoke.ultra.datetime

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import kotlin.math.floor
import kotlin.time.Instant

/**
 * Zone math for [MpTimezone], with java.time semantics on every target.
 *
 * Only "offset at an instant" is platform specific ([platformOffsetSecondsAt]). Everything else is derived here
 * from it plus UTC-only kotlinx calls, so the JS target needs no kotlinx time zone data.
 */
internal object TimezoneEngine {

    // Epoch seconds as Double and the date split in EpochMath, not Instant ± Duration or kotlinx's own conversion: on
    // JS every Long is emulated, and this code runs on every conversion. Exact for ±1 million years (EpochMath).
    private const val PROBE_WINDOW_SECONDS = 86_400.0

    /** The offset of [timezone] at [instant], in seconds. */
    fun offsetSecondsAt(timezone: MpTimezone, instant: Instant): Int {
        return offsetSecondsAt(timezone, instant.epochSeconds.toDouble())
    }

    private fun offsetSecondsAt(timezone: MpTimezone, epochSeconds: Double): Int {
        return fixedOffsetSecondsOrNull(timezone.id) ?: platformOffsetSecondsAt(timezone.id, epochSeconds)
    }

    /** The wall-clock time of [instant] in [timezone]. */
    fun toLocalDateTime(timezone: MpTimezone, instant: Instant): LocalDateTime {
        val seconds = instant.epochSeconds.toDouble()

        return EpochMath.localDateTime(seconds + offsetSecondsAt(timezone, seconds), instant.nanosecondsOfSecond)
    }

    /**
     * The instant of the wall-clock time [local] in [timezone], like java.time's `ZonedDateTime.ofLocal`.
     *
     * In a gap the result moves forward by the length of the gap. In an overlap [preferredOffsetSeconds] wins when
     * it is one of the two valid offsets, otherwise the earlier offset does.
     */
    fun toInstant(timezone: MpTimezone, local: LocalDateTime, preferredOffsetSeconds: Int? = null): Instant {
        val wall = EpochMath.epochSeconds(local)

        fixedOffsetSecondsOrNull(timezone.id)?.let { return EpochMath.instant(wall - it, local.nanosecond) }

        val offset = when (val resolution = resolve(timezone.id, wall)) {
            is Resolution.Unique -> resolution.offset
            is Resolution.Overlap -> when (preferredOffsetSeconds) {
                resolution.later -> resolution.later
                else -> resolution.earlier
            }
            // Using the offset from before the transition shifts the wall time forward by the gap length.
            is Resolution.Gap -> resolution.before
        }

        return EpochMath.instant(wall - offset, local.nanosecond)
    }

    /** The first instant of [date] in [timezone]: midnight, or the end of the gap when midnight does not exist. */
    fun startOfDay(timezone: MpTimezone, date: LocalDate): Instant {
        val wall = EpochMath.epochSeconds(date)

        fixedOffsetSecondsOrNull(timezone.id)?.let { return EpochMath.instant(wall - it, 0) }

        val start = when (val resolution = resolve(timezone.id, wall)) {
            is Resolution.Unique -> wall - resolution.offset
            is Resolution.Overlap -> wall - resolution.earlier
            is Resolution.Gap -> transitionAfter(timezone.id, wall - PROBE_WINDOW_SECONDS, resolution.before)
        }

        return EpochMath.instant(start, 0)
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
                val seconds = instant.epochSeconds.toDouble()
                val offset = offsetSecondsAt(timezone, seconds)
                val local = EpochMath.localDateTime(seconds + offset, instant.nanosecondsOfSecond)
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
     * Resolves the wall-clock time [wall] (epoch seconds as if it were UTC) in the zone [zoneId].
     *
     * Assumes no two transitions within two days of each other (pinned by `OffsetIntervalCacheJvmSpec`).
     */
    private fun resolve(zoneId: String, wall: Double): Resolution {
        val before = platformOffsetSecondsAt(zoneId, wall - PROBE_WINDOW_SECONDS)
        val after = platformOffsetSecondsAt(zoneId, wall + PROBE_WINDOW_SECONDS)

        fun isValid(offset: Int) = platformOffsetSecondsAt(zoneId, wall - offset) == offset

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

    /** The first second after [from] at which [zoneId] stops using the offset [before]. */
    private fun transitionAfter(zoneId: String, from: Double, before: Int): Double {
        var lo = from
        var hi = from + 2 * PROBE_WINDOW_SECONDS

        while (hi - lo > 1.0) {
            val mid = floor((lo + hi) / 2.0)

            if (platformOffsetSecondsAt(zoneId, mid) == before) lo = mid else hi = mid
        }

        return hi
    }

    /**
     * The offset of a fixed-offset zone id, or null for a region id.
     *
     * Accepts what java.time's `ZoneId.of` accepts for fixed offsets (`Z` never arrives: `MpTimezone.of` maps it to
     * `UTC`): `UTC`, `GMT`, `UT`, a bare offset
     * (`+h`, `+hh`, `+hh:mm`, `+hhmm`, `+hh:mm:ss`, `+hhmmss`) and those offsets after a `UTC`, `GMT` or `UT` prefix.
     */
    internal fun fixedOffsetSecondsOrNull(zoneId: String): Int? {
        val offset = when {
            zoneId.startsWith("+") || zoneId.startsWith("-") -> zoneId
            else -> {
                val prefix = fixedOffsetPrefixes.firstOrNull { zoneId.startsWith(it) } ?: return null
                val rest = zoneId.removePrefix(prefix).ifEmpty { return 0 }
                // e.g. "GMT0" is a region id, not an offset
                if (!rest.startsWith("+") && !rest.startsWith("-")) return null
                rest
            }
        }

        return parseOffsetSeconds(offset) ?: throw IllegalTimeZoneException("Invalid zone id: $zoneId")
    }

    // "UTC" before "UT", so the longer prefix wins
    private val fixedOffsetPrefixes = arrayOf("UTC", "GMT", "UT")

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

        if (minutes > 59 || seconds > 59) return null

        val total = hours * 3600 + minutes * 60 + seconds
        if (total > 18 * 3600) return null

        return if (sign == "-") -total else total
    }
}

/**
 * The offset of the region zone [zoneId] at [epochSeconds] (whole seconds as a `Double`, so JS needs no emulated
 * `Long`), in seconds. Throws [IllegalTimeZoneException] if unknown.
 */
internal expect fun platformOffsetSecondsAt(zoneId: String, epochSeconds: Double): Int

/** The id of the system's time zone. */
internal expect fun platformSystemDefaultZoneId(): String
