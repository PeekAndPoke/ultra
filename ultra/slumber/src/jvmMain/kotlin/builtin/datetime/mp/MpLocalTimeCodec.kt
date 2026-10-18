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
        //  is accepted and silently wrapped/corrupted by ofMilliSeconds()'s modulo (e.g. -1000 awakes
        //  to an MpLocalTime with second == -1) instead of awaking to null.
        return MpLocalTime.ofMilliSeconds(data.toLong())
    }
}

/** Slumberer for [MpLocalTime] values. Writes the milliseconds-of-day as a [Long], no wrapper map. */
object MpLocalTimeSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Long? {

        if (data !is MpLocalTime) {
            return null
        }

        return data.inWholeMilliSeconds()
    }
}
