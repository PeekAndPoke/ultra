package de.peekandpoke.ultra.common.datetime

internal const val IsoFormatString = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

const val GenesisTimestamp = -377_736_739_200_000
const val DoomsdayTimestamp = 253_402_300_800_000

val Genesis = PortableDate(GenesisTimestamp)
val Doomsday = PortableDate(DoomsdayTimestamp)

val GenesisDateTime = PortableDateTime(GenesisTimestamp)
val DoomsdayDateTime = PortableDateTime(DoomsdayTimestamp)

expect fun PortableDate.toIsoString(): String

expect fun PortableDateTime.toIsoString(): String
