package de.peekandpoke.ultra.common.datetime

import java.time.*
import java.time.format.DateTimeFormatter

private val isoFormat: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

internal val utc = ZoneId.of("UTC")

val PortableDate.date
    get(): LocalDate = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(timestamp),
        utc
    ).toLocalDate()

actual fun PortableDate.toIsoString(): String = date.format(isoFormat)

val LocalDate.portable
    get(): PortableDate = PortableDate(
        timestamp = this.atStartOfDay(utc).toInstant().toEpochMilli()
    )

val PortableDateTime.date
    get(): LocalDateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(timestamp),
        utc
    )

actual fun PortableDateTime.toIsoString(): String = date.format(isoFormat)

val LocalDateTime.portable
    get(): PortableDateTime = PortableDateTime(
        timestamp = this.atZone(utc).toInstant().toEpochMilli()
    )

val ZonedDateTime.portable
    get(): PortableDateTime = PortableDateTime(
        timestamp = this.toInstant().toEpochMilli()
    )
