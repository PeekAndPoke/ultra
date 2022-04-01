package de.peekandpoke.ultra.common.datetime

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime


fun PortableDate.Companion.now(): PortableDate =
    LocalDate.now().portable

val PortableDate.date
    get(): LocalDate = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(timestamp),
        utc
    ).toLocalDate()

val LocalDate.portable
    get(): PortableDate = PortableDate(
        timestamp = this.atStartOfDay(utc).toInstant().toEpochMilli()
    )
