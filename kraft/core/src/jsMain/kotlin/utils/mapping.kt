package de.peekandpoke.kraft.utils

import de.peekandpoke.ultra.common.datetime.MpLocalDate
import de.peekandpoke.ultra.common.datetime.MpLocalDateTime
import de.peekandpoke.ultra.common.datetime.MpLocalTime
import de.peekandpoke.ultra.common.datetime.MpZonedDateTime

fun <T> identity(it: T): T = it

fun stringToString(it: String?): String = it ?: ""

fun numberToString(it: Number?): String = it?.toString() ?: ""

fun stringToInt(it: String): Int = if (it.isBlank()) 0 else it.toInt()

fun stringToFloat(it: String): Float = if (it.isBlank()) 0.0f else it.toFloat()

fun stringToDouble(it: String): Double = if (it.isBlank()) 0.0 else it.toDouble()

fun dateToYmd(it: MpLocalDate?): String = it?.format("yyyy-MM-dd") ?: ""

fun stringToDate(it: String): MpLocalDate = MpLocalDate.parse(it)
fun stringToDateOrNull(it: String): MpLocalDate? = MpLocalDate.tryParse(it)

fun localDateTimeToYmdHms(it: MpLocalDateTime?): String = zonedDateTimeToYmdHms(it?.atUTC())

fun stringToLocalDateTime(it: String): MpLocalDateTime = MpLocalDateTime.parse(it)
fun stringToLocalDateTimeOrNull(it: String): MpLocalDateTime? = MpLocalDateTime.tryParse(it)

fun zonedDateTimeToYmdHms(it: MpZonedDateTime?): String = it?.format("yyyy-MM-ddTHH:mm:ss") ?: ""

fun stringToZonedDateTime(it: String): MpZonedDateTime = MpZonedDateTime.parse(it)
fun stringToZonedDateTimeOrNull(it: String): MpZonedDateTime? = MpZonedDateTime.tryParse(it)

fun timeToHms(it: MpLocalTime?): String = it?.format("HH:mm:ss") ?: ""

fun stringToLocalTime(it: String): MpLocalTime = MpLocalTime.parse(it)
fun stringToLocalTimeOrNull(it: String): MpLocalTime? = MpLocalTime.tryParse(it)

