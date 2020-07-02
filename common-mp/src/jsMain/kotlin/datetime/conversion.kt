package de.peekandpoke.common.datetime

import com.soywiz.klock.Date
import com.soywiz.klock.DateTime

val PortableDate.date get(): Date = DateTime.fromUnix(timestamp).date

val PortableDateTime.date get(): DateTime = DateTime.fromUnix(timestamp)
