package io.peekandpoke.ultra.slumber.builtin.datetime

import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Shared utilities for date/time codec serialization and deserialization. */

// TODO(scan): unused - not referenced anywhere in this module; dead code candidate for removal.
internal val format: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

/** The `java.time` UTC zone, used by the `javatime` and `mp` codecs as their fixed slumbering zone. */
internal val utc = ZoneId.of("UTC")

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
