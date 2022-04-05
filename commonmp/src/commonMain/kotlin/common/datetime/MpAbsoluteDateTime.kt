package de.peekandpoke.ultra.common.datetime

import kotlinx.datetime.TimeZone

interface MpAbsoluteDateTime {

    fun toInstant(): MpInstant

    fun atZone(timezone: TimeZone): MpZonedDateTime {
        return toInstant().atZone(timezone)
    }
}
