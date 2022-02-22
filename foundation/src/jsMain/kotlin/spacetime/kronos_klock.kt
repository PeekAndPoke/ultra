package de.peekandpoke.ultra.foundation.spacetime

import com.soywiz.klock.Date
import com.soywiz.klock.DateTime
import com.soywiz.klock.Time

// How to format times in timezones?
//
// See:
// https://kangax.github.io/compat-table/esintl/#test-DateTimeFormat_accepts_IANA_timezone_names
//
// And:
// https://stackoverflow.com/questions/15141762/how-to-initialize-a-javascript-date-to-a-particular-time-zone

fun Kronos.timeNow(): Time = dateTimeNow().time

fun Kronos.dateNow(): Date = dateTimeNow().date

fun Kronos.dateTimeNow(): DateTime = DateTime.fromUnix(millisNow())
