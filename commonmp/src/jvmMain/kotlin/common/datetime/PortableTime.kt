package de.peekandpoke.ultra.common.datetime

import java.time.LocalTime

val LocalTime.portable
    get(): PortableTime = PortableTime(
        milliSeconds = 1000L * (hour * 60 * 60 + minute * 60 + second)
    )

val PortableTime.asLocalTime
    get(): LocalTime = LocalTime.ofSecondOfDay(milliSeconds / 1000)
