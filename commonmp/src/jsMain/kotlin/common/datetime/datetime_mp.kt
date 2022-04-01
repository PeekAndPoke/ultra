package de.peekandpoke.ultra.common.datetime

import com.soywiz.klock.DateFormat

private val isoFormat = DateFormat(ISO_FORMAT)

actual fun PortableDate.toIsoString(): String = date.format(isoFormat)

actual fun PortableDateTime.toIsoString(): String = date.format(isoFormat)
