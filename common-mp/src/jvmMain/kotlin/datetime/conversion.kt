package de.peekandpoke.common.datetime

import java.time.*

internal val utc = ZoneId.of("UTC")

val PortableDate.date
    get(): LocalDate = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(timestamp),
        utc
    ).toLocalDate()

val LocalDate.portable
    get(): PortableDate = PortableDate(
        timestamp = this.atStartOfDay(utc).toInstant().toEpochMilli()
    )

val PortableDateTime.date
    get(): LocalDateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(timestamp),
        utc
    )

val LocalDateTime.portable
    get(): PortableDateTime = PortableDateTime(
        timestamp = this.atZone(utc).toInstant().toEpochMilli()
    )

val ZonedDateTime.portable
    get(): PortableDateTime = PortableDateTime(
        timestamp = this.toInstant().toEpochMilli()
    )
