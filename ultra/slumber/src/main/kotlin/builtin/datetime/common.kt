package de.peekandpoke.ultra.slumber.builtin.datetime

import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal val format: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

internal val utc = ZoneId.of("UTC")

internal const val TS = "ts"
internal const val TIMEZONE = "timezone"
internal const val HUMAN = "human"

internal fun toMap(ts: Long, timezone: ZoneId, human: String) = toMap(
    ts = ts,
    timezone = timezone.id,
    human = human,
)

internal fun toMap(ts: Long, timezone: String, human: String) = mapOf(
    TS to ts,
    TIMEZONE to timezone,
    HUMAN to human
)
