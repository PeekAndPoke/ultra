package de.peekandpoke.ultra.common.datetime

import com.soywiz.klock.Date
import com.soywiz.klock.DateTime

val GenesisDateTime: DateTime = DateTime.fromUnix(GENESIS_TIMESTAMP)
val DoomsdayDateTime: DateTime = DateTime.fromUnix(DOOMSDAY_TIMESTAMP)

val GenesisDate: Date = GenesisDateTime.date
val DoomsdayDate: Date = DoomsdayDateTime.date
