package io.peekandpoke.ultra.datetime

import kotlinx.datetime.TimeZone

interface MpAbsoluteDateTime {

    /**
     * Converts to an [MpInstant].
     */
    fun toInstant(): MpInstant

    /**
     * Converts to an [MpZonedDateTime] at the given [timezone].
     */
    fun atZone(timezone: TimeZone): MpZonedDateTime {
        return toInstant().atZone(timezone)
    }

    /**
     * Converts to an [MpZonedDateTime] at the given [timezone].
     */
    fun atZone(timezone: MpTimezone): MpZonedDateTime {
        return atZone(timezone.kotlinx)
    }

    /**
     * Converts to an [MpZonedDateTime] at UTC.
     */
    fun atUTC(): MpZonedDateTime {
        return atZone(TimeZone.UTC)
    }

    /**
     * Converts to an [MpZonedDateTime] at the systems current timezone.
     */
    fun atSystemDefaultZone(): MpZonedDateTime {
        return atZone(TimeZone.currentSystemDefault())
    }
}
