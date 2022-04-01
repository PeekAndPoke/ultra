package de.peekandpoke.ultra.common.datetime

import com.soywiz.klock.Date
import com.soywiz.klock.DateTime
import com.soywiz.klock.Time


val PortableDate.date get(): Date = DateTime.fromUnix(timestamp).date

val PortableDateTime.date get(): DateTime = DateTime.fromUnix(timestamp)

// FROM Klock to Portable //////////////////////////////////////////////////////////////////////////////////////////////

val Time.portable get(): PortableTime = PortableTime(encoded.millisecondsLong)

val Date.portable get(): PortableDate = portable(Time(0, 0, 0))

fun Date.portable(time: Time = Time(0, 0, 0)): PortableDate = PortableDate(
    DateTime(this, time).unixMillisLong
)

val DateTime.portable get(): PortableDateTime = PortableDateTime(unixMillisLong)
