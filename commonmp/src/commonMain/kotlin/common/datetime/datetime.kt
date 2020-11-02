package de.peekandpoke.ultra.common.datetime

internal const val isoFormatString = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

expect fun PortableDate.toIsoString(): String

expect fun PortableDateTime.toIsoString(): String
