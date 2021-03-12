package de.peekandpoke.ultra.common.datetime

internal const val ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

const val GENESIS_TIMESTAMP = -377_736_739_200_000
const val DOOMSDAY_TIMESTAMP = 253_402_300_800_000

val Genesis = PortableDate(GENESIS_TIMESTAMP)
val Doomsday = PortableDate(DOOMSDAY_TIMESTAMP)

val GenesisDateTime = PortableDateTime(GENESIS_TIMESTAMP)
val DoomsdayDateTime = PortableDateTime(DOOMSDAY_TIMESTAMP)

expect fun PortableDate.toIsoString(): String

expect fun PortableDateTime.toIsoString(): String
