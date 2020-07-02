package de.peekandpoke.ultra.slumber.builtin.datetime

import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal val format: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

internal val utc = ZoneId.of("UTC")

internal const val TS = "ts"
internal const val TIMEZONE = "timezone"
internal const val HUMAN = "human"

internal fun toMap(ts: Long, timezone: ZoneId, human: String) = mapOf(
    TS to ts,
    TIMEZONE to timezone.id,
    HUMAN to human
)
