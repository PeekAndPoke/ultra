package de.peekandpoke.ultra.common.datetime

import com.soywiz.klock.Date
import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.klock.Time

private val isoFormat = DateFormat(isoFormatString)

val PortableDate.date get(): Date = DateTime.fromUnix(timestamp).date

actual fun PortableDate.toIsoString(): String = date.format(isoFormat)

val PortableDateTime.date get(): DateTime = DateTime.fromUnix(timestamp)

actual fun PortableDateTime.toIsoString(): String = date.format(isoFormat)

// FROM Klock to Portable //////////////////////////////////////////////////////////////////////////////////////////////

val Date.portable get(): PortableDate = portable(Time(12, 0, 0))

fun Date.portable(time: Time = Time(12, 0, 0)): PortableDate = PortableDate(
    DateTime(this, time).unixMillisLong
)

val DateTime.portable
    get(): PortableDateTime = PortableDateTime(
        unixMillisLong
    )
