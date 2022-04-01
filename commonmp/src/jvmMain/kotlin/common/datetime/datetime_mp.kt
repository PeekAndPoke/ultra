package de.peekandpoke.ultra.common.datetime

import java.time.format.DateTimeFormatter

actual fun PortableDate.toIsoString(): String = date.format(DateTimeFormatter.ISO_DATE)

actual fun PortableDateTime.toIsoString(): String = date.format(DateTimeFormatter.ISO_DATE_TIME)


