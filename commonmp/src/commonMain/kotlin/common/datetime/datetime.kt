package de.peekandpoke.ultra.common.datetime

internal const val ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

/**
 * The earliest timestamp used is considered to be '-10000-01-01T00:00:00Z'
 */
const val GENESIS_TIMESTAMP = -377_736_739_200_000

/**
 * The latest timestamp used is considered to be '+10000-01-01T00:00:00Z'
 */
const val DOOMSDAY_TIMESTAMP = 253_402_300_800_000

val PortableGenesisDate = PortableDate(GENESIS_TIMESTAMP)
val PortableDoomsdayDate = PortableDate(DOOMSDAY_TIMESTAMP)

val PortableGenesisDateTime = PortableDateTime(GENESIS_TIMESTAMP)
val PortableDoomsdayDateTime = PortableDateTime(DOOMSDAY_TIMESTAMP)

expect fun PortableDate.toIsoString(): String

expect fun PortableDateTime.toIsoString(): String
