package de.peekandpoke.ultra.slumber.builtin.datetime

import com.soywiz.klock.DateFormat
import java.time.ZoneId

internal val utc = ZoneId.of("UTC")

internal const val TS = "ts"
internal const val TIMEZONE = "timezone"
internal const val HUMAN = "human"

internal val klockDateFormat = DateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")

internal fun toMap(ts: Long, timezone: ZoneId, human: String) = mapOf(
    TS to ts,
    TIMEZONE to timezone.id,
    HUMAN to human
)
