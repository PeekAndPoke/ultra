package io.peekandpoke.ultra.slumber.builtin.datetime.mp

import io.peekandpoke.ultra.datetime.MpLocalTime
import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer

/** Awaker for [MpLocalTime] values. Reads a milliseconds-of-day [Long]. */
object MpLocalTimeAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): MpLocalTime? {

        if (data !is Number) {
            return null
        }

        // TODO(scan): different unit than the javatime sibling (LocalTimeCodec.kt: seconds-of-day) -
        //  the same wall-clock time serializes 1000x apart depending on which codec produced it.
        // TODO(scan): no range validation, unlike the javatime sibling. A negative or >= one-day value
        //  is accepted and wrapped into the day (e.g. -1000 awakes as 23:59:59) instead of awaking to null.
        // Exactly one day is 24:00 (MpLocalTime.EndOfDay), compared as a Double so a fraction like 86400000.5 does not
        // truncate into it; every other value wraps into the day, as before.
        if (data.toDouble() == MpLocalTime.MILLIS_PER_DAY.toDouble()) return MpLocalTime.EndOfDay

        return MpLocalTime.ofMilliSeconds(data.toLong().mod(MpLocalTime.MILLIS_PER_DAY.toLong()).toInt())
    }
}

/** Slumberer for [MpLocalTime] values. Writes the milliseconds-of-day as a [Long], no wrapper map. */
object MpLocalTimeSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Long? {

        if (data !is MpLocalTime) {
            return null
        }

        // Still a Long, so the stored number type does not change with the Int storage.
        return data.inWholeMilliSeconds().toLong()
    }
}
