@file:JvmName("ConstantsJvm")

package de.peekandpoke.ultra.common.datetime

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

internal val utc = ZoneOffset.UTC

val GenesisInstant: Instant = Instant.ofEpochMilli(GENESIS_TIMESTAMP)
val DoomsdayInstant: Instant = Instant.ofEpochMilli(DOOMSDAY_TIMESTAMP)

val GenesisLocalDateTime: LocalDateTime = LocalDateTime.ofInstant(GenesisInstant, utc)
val DoomsdayLocalDateTime: LocalDateTime = LocalDateTime.ofInstant(DoomsdayInstant, utc)

val GenesisLocalDate: LocalDate = GenesisLocalDateTime.toLocalDate()
val DoomsdayLocalDate: LocalDate = DoomsdayLocalDateTime.toLocalDate()

