package de.peekandpoke.ultra.common.datetime

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val isoFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'00:00:00")

fun PortableDate.Companion.now(): PortableDate =
    LocalDate.now().portable

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

