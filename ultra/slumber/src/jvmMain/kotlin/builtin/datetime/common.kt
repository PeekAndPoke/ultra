package io.peekandpoke.ultra.slumber.builtin.datetime

import io.peekandpoke.ultra.datetime.MpTimezone
import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.datetime.TimeZone
import java.time.DateTimeException
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Shared utilities for date/time codec serialization and deserialization. */

// TODO(scan): unused - not referenced anywhere in this module; dead code candidate for removal.
internal val format: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

/** The `java.time` UTC zone, used by the `javatime` and `mp` codecs as their fixed slumbering zone. */
internal val utc = ZoneId.of("UTC")

/*
 * The zone a `timezone` entry names, or null: for a missing, non-string or unknown zone. An awaker returns null for
 * input it cannot read, and Slumber then refuses it with an `AwakerException` naming the path. The zone factories
 * THROW instead, which a request body turned into a 500 (ULTRA-00294).
 */

/** The `java.time` zone [id] names; null when it is no String or names no zone. */
internal fun javaZoneOrNull(id: Any?): ZoneId? = (id as? String)?.let {
    try {
        ZoneId.of(it)
    } catch (_: DateTimeException) {
        null
    }
}

/** The kotlinx zone [id] names; null when it is no String or names no zone. */
internal fun kotlinxZoneOrNull(id: Any?): TimeZone? = (id as? String)?.let {
    try {
        TimeZone.of(it)
    } catch (_: IllegalTimeZoneException) {
        null
    }
}

/** The [MpTimezone] [id] names; null when it is no String or names no zone that kotlinx, which computes with it, knows. */
internal fun mpTimezoneOrNull(id: Any?): MpTimezone? = (id as? String)
    ?.let { MpTimezone.of(it) }
    ?.takeIf { kotlinxZoneOrNull(it.id) != null }

/** Map key for the epoch-millisecond timestamp. */
internal const val TS = "ts"

/** Map key for the zone id string. */
internal const val TIMEZONE = "timezone"

/** Map key for the debug-only human-readable rendering; never read back by any Awaker. */
internal const val HUMAN = "human"

/** Builds the shared `{ts, timezone, human}` wire map, converting [timezone] to its zone id string. */
internal fun toMap(ts: Long, timezone: ZoneId, human: String) = toMap(
    ts = ts,
    timezone = timezone.id,
    human = human,
)

/** Builds the shared `{ts, timezone, human}` wire map used by the date/time slumberers in this package. */
internal fun toMap(ts: Long, timezone: String, human: String) = mapOf(
    TS to ts,
    TIMEZONE to timezone,
    HUMAN to human
)
