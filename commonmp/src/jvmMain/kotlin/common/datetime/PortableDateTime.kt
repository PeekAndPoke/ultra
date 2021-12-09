package de.peekandpoke.ultra.common.datetime

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val isoFormat: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

fun PortableDateTime.Companion.now(): PortableDateTime = LocalDateTime.now().portable

val PortableDateTime.date
    get(): LocalDateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(timestamp),
        utc
    )

actual fun PortableDateTime.toIsoString(): String = date.format(isoFormat)

val Instant.portable
    get(): PortableDateTime = PortableDateTime(
        timestamp = this.toEpochMilli()
    )

val LocalDateTime.portable
    get(): PortableDateTime = PortableDateTime(
        timestamp = this.atZone(utc).toInstant().toEpochMilli()
    )

val ZonedDateTime.portable
    get(): PortableDateTime = PortableDateTime(
        timestamp = this.toInstant().toEpochMilli()
    )
