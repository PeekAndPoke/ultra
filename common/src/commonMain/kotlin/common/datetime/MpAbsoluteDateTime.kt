package de.peekandpoke.ultra.common.datetime

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
    // TODO: test me for all implementing classes
    fun atUTC(): MpZonedDateTime {
        return atZone(TimeZone.UTC)
    }

    /**
     * Converts to an [MpZonedDateTime] at the systems current timezone.
     */
    // TODO: test me for all implementing classes
    fun atSystemDefaultZone(): MpZonedDateTime {
        return atZone(TimeZone.currentSystemDefault())
    }
}
