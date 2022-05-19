@file:Suppress("Detekt:LongParameterList", "Detekt:ParameterListWrapping")

package de.peekandpoke.ultra.common.datetime

import kotlinx.datetime.TimeZone
import kotlin.jvm.JvmName

fun ts(
    fromHour: Int, fromMinute: Int, toHour: Int, toMinute: Int
) = MpLocalTimeSlot(
    from = MpLocalTime.of(hour = fromHour, minute = fromMinute),
    to = MpLocalTime.of(hour = toHour, minute = toMinute),
)

fun ts(
    fromHour: Int, fromMinute: Int, fromSecond: Int, toHour: Int, toMinute: Int, toSecond: Int
) = MpLocalTimeSlot(
    from = MpLocalTime.of(hour = fromHour, minute = fromMinute, second = fromSecond),
    to = MpLocalTime.of(hour = toHour, minute = toMinute, second = toSecond),
)

@JvmName("format_List_MpLocalTimeSlot")
fun List<MpLocalTimeSlot>.format() = map { it.formatHhMmSs() }.toString()

fun MpInstantRange.format() = atSystemDefaultZone().formatDdMmmYyyyHhMm()

@JvmName("format_List_MpInstantRange")
fun List<MpInstantRange>.format() = map { it.format() }.toString()

fun MpZonedDateTimeRange.format() = formatDdMmmYyyyHhMm()

@JvmName("format_List_MpZonedDateTimeRange")
fun List<MpZonedDateTimeRange>.format() = map { it.format() }.toString()

object TestConstants {

    const val tsUTC_20220405_000000: Long = 1649116800000L

    const val tsUtc_20220405_121314: Long = 1649160794000L

    val tsBucharest_20220405_121314: Long = (tsUtc_20220405_121314 -
            TimeZone.of("Europe/Bucharest").offsetMillisAt(
                MpInstant.fromEpochMillis(tsUtc_20220405_121314)
            ))
}
