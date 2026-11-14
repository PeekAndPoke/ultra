package io.peekandpoke.ultra.slumber.builtin.datetime.mp

import io.peekandpoke.ultra.datetime.MpLocalTimeSlot
import io.peekandpoke.ultra.datetime.toRawData
import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer
import kotlin.time.Duration.Companion.milliseconds

/**
 * Awaker for [MpLocalTimeSlot] values. Reads `{from, duration, to}` in milliseconds; without `duration` the old
 * `{from, to}` (ULTRA-00341). Null when `from` or both `duration` and `to` are missing, or a value is not a number.
 */
object MpLocalTimeSlotAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): MpLocalTimeSlot? {

        if (data !is Map<*, *>) {
            return null
        }

        // from and to as MpLocalTime values are read: exactly one day is 24:00, every other number wraps
        val from = data["from"]?.let { MpLocalTimeAwaker.awake(it, context) } ?: return null

        return when (val duration = data["duration"]) {
            is Number -> MpLocalTimeSlot.of(from = from, duration = duration.toLong().milliseconds)
            null -> data["to"]?.let { MpLocalTimeAwaker.awake(it, context) }?.let { MpLocalTimeSlot.of(from, it) }
            else -> null
        }
    }
}

/** Slumberer for [MpLocalTimeSlot] values. Writes `{from, duration, to}` in milliseconds, `to` derived. */
object MpLocalTimeSlotSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any>? {

        if (data !is MpLocalTimeSlot) {
            return null
        }

        val raw = data.toRawData()

        return mapOf(
            "from" to raw.from,
            "duration" to (raw.duration ?: 0L),
            "to" to (raw.to ?: raw.from),
        )
    }
}
