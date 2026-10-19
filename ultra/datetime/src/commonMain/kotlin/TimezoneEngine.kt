package io.peekandpoke.ultra.datetime

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import kotlin.math.abs
import kotlin.time.Instant

/**
 * Zone math for [MpTimezone], with java.time semantics on every target.
 *
 * Only "offset at an instant" is platform specific ([platformOffsetSecondsAt]). Everything else is derived here
 * from it plus UTC-only kotlinx calls, so the JS target needs no kotlinx time zone data.
 */
internal object TimezoneEngine {

    // Epoch seconds as Double and the date split in EpochMath, not Instant ± Duration or kotlinx's own conversion: on
    // JS every Long is emulated, and this code runs on every conversion. A Double holds every whole second only up to
    // 2^53, so from 2^52 on the zone lookups take the exact Long path (JVM/native; JS never gets that far — Intl's
    // range ends at ±8.64e12 s). Results are built by EpochMath from exact values.
    private const val PROBE_WINDOW_SECONDS = 86_400
    private const val EXACT_AS_DOUBLE = 4_503_599_627_370_496.0 // 2^52

    /** The offset of [timezone] at [instant], in seconds. */
    fun offsetSecondsAt(timezone: MpTimezone, instant: Instant): Int {
        return fixedOffsetSecondsOrNull(timezone.id) ?: lookup(timezone.id, instant.epochSeconds)
    }

    /** The wall-clock time of [instant] in [timezone]. */
    fun toLocalDateTime(timezone: MpTimezone, instant: Instant): LocalDateTime {
        return EpochMath.localDateTime(
            epochSeconds = instant.epochSeconds,
            offsetSeconds = offsetSecondsAt(timezone, instant),
            nanos = instant.nanosecondsOfSecond,
        )
    }

    /**
     * The instant of the wall-clock time [local] in [timezone], like java.time's `ZonedDateTime.ofLocal`.
     *
     * In a gap the result moves forward by the length of the gap. In an overlap [preferredOffsetSeconds] wins when
     * it is one of the two valid offsets, otherwise the earlier offset does.
     */
    fun toInstant(timezone: MpTimezone, local: LocalDateTime, preferredOffsetSeconds: Int? = null): Instant {
        fixedOffsetSecondsOrNull(timezone.id)?.let { return EpochMath.instant(local, it) }

        val wall = EpochMath.epochSeconds(local)

        val resolution = when {
            abs(wall) < EXACT_AS_DOUBLE -> resolve { platformOffsetSecondsAt(timezone.id, wall + it) }
            else -> EpochMath.instant(local, 0).epochSeconds.let { exact -> resolve { lookup(timezone.id, exact + it) } }
        }

        val offset = when (resolution) {
            is Resolution.Unique -> resolution.offset
            is Resolution.Overlap -> when (preferredOffsetSeconds) {
                resolution.later -> resolution.later
                else -> resolution.earlier
            }
            // Using the offset from before the transition shifts the wall time forward by the gap length.
            is Resolution.Gap -> resolution.before
        }

        return EpochMath.instant(local, offset)
    }

    /** The first instant of [date] in [timezone]: midnight, or the end of the gap when midnight does not exist. */
    fun startOfDay(timezone: MpTimezone, date: LocalDate): Instant {
        fixedOffsetSecondsOrNull(timezone.id)?.let { return EpochMath.instant(date, it) }

        val wall = EpochMath.epochSeconds(date)

        val resolution = when {
            abs(wall) < EXACT_AS_DOUBLE -> resolve { platformOffsetSecondsAt(timezone.id, wall + it) }
            else -> EpochMath.instant(date, 0).epochSeconds.let { exact -> resolve { lookup(timezone.id, exact + it) } }
        }

        return when (resolution) {
            is Resolution.Unique -> EpochMath.instant(date, resolution.offset)
            is Resolution.Overlap -> EpochMath.instant(date, resolution.earlier)
            is Resolution.Gap -> {
                val midnight = EpochMath.instant(date, 0).epochSeconds
                Instant.fromEpochSeconds(transitionAfter(timezone.id, midnight - PROBE_WINDOW_SECONDS, resolution.before))
            }
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
                val local = EpochMath.localDateTime(instant.epochSeconds, offset, instant.nanosecondsOfSecond)
                val shifted = LocalDateTime(local.date.plus(value, unit), local.time)

                toInstant(timezone, shifted, preferredOffsetSeconds = offset)
            }
        }
    }

    /** The offset of the region zone [zoneId] at [epochSeconds]: fast as a Double where that is exact, else exact. */
    private fun lookup(zoneId: String, epochSeconds: Long): Int {
        val asDouble = epochSeconds.toDouble()

        return when {
            abs(asDouble) < EXACT_AS_DOUBLE -> platformOffsetSecondsAt(zoneId, asDouble)
            else -> platformOffsetSecondsAtExact(zoneId, epochSeconds)
        }
    }

    private sealed interface Resolution {
        data class Unique(val offset: Int) : Resolution

        /** [earlier] is the offset before the transition, i.e. the one giving the earlier instant. */
        data class Overlap(val earlier: Int, val later: Int) : Resolution

        data class Gap(val before: Int) : Resolution
    }

    /**
     * Resolves a wall-clock time in a zone, given [offsetAt] — the zone's offset at the wall time read as UTC, shifted by
     * the given number of seconds.
     *
     * Assumes no two transitions within two days of each other (pinned by `OffsetIntervalCacheJvmSpec`).
     */
    private inline fun resolve(offsetAt: (shiftSeconds: Int) -> Int): Resolution {
        val before = offsetAt(-PROBE_WINDOW_SECONDS)
        val after = offsetAt(PROBE_WINDOW_SECONDS)

        val validBefore = offsetAt(-before) == before

        if (before == after) {
            return Resolution.Unique(if (validBefore) before else offsetAt(0))
        }

        val validAfter = offsetAt(-after) == after

        return when {
            validBefore && validAfter -> Resolution.Overlap(earlier = before, later = after)
            validBefore -> Resolution.Unique(before)
            validAfter -> Resolution.Unique(after)
            else -> Resolution.Gap(before = before)
        }
    }

    /**
     * The first second after [from] at which [zoneId] stops using the offset [before].
     *
     * Bisects on Long, not Double: beyond 2^53 a Double midpoint can round onto a bound and never make progress. This
     * runs only for a start of day inside a gap, so the emulated Long on JS does not matter.
     */
    private fun transitionAfter(zoneId: String, from: Long, before: Int): Long {
        var lo = from
        var hi = from + 2 * PROBE_WINDOW_SECONDS

        while (hi - lo > 1) {
            val mid = lo + (hi - lo) / 2

            if (lookup(zoneId, mid) == before) lo = mid else hi = mid
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

/** Like [platformOffsetSecondsAt], exact for any [epochSeconds] (used from 2^52 on, where a Double is not). */
internal expect fun platformOffsetSecondsAtExact(zoneId: String, epochSeconds: Long): Int

/** The id of the system's time zone. */
internal expect fun platformSystemDefaultZoneId(): String
