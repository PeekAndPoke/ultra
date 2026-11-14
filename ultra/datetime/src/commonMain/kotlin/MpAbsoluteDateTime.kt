package io.peekandpoke.ultra.datetime

import kotlinx.datetime.TimeZone

interface MpAbsoluteDateTime {

    /**
     * Converts to an [MpInstant].
     */
    fun toInstant(): MpInstant

    /** Whether [other] is the same point in time, whatever the zone; unlike equals, which also compares the zone. */
    fun isSameInstant(other: MpAbsoluteDateTime): Boolean {
        return toInstant() == other.toInstant()
    }

    /**
     * Converts to an [MpZonedDateTime] at the given [timezone].
     */
    fun atZone(timezone: TimeZone): MpZonedDateTime {
        return atZone(timezone.mp)
    }

    /**
     * Converts to an [MpZonedDateTime] at the given [timezone].
     */
    fun atZone(timezone: MpTimezone): MpZonedDateTime {
        return toInstant().atZone(timezone)
    }

    /**
     * Converts to an [MpZonedDateTime] at UTC.
     */
    fun atUTC(): MpZonedDateTime {
        return atZone(MpTimezone.UTC)
    }

    /**
     * Converts to an [MpZonedDateTime] at the systems current timezone.
     */
    fun atSystemDefaultZone(): MpZonedDateTime {
        return atZone(MpTimezone.systemDefault)
    }
}
