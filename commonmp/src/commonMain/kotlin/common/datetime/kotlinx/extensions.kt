package de.peekandpoke.ultra.common.datetime.kotlinx

import de.peekandpoke.ultra.common.datetime.MpInstant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.offsetAt as instantOffsetAt

val UtcOffset.totalMillis: Long get() = totalSeconds * 1000L

fun TimeZone.offsetAt(instant: MpInstant): UtcOffset = instantOffsetAt(instant.value)

fun TimeZone.offsetSecondsAt(instant: MpInstant): Int = offsetAt(instant).totalSeconds

fun TimeZone.offsetMillisAt(instant: MpInstant): Long = offsetAt(instant).totalMillis
