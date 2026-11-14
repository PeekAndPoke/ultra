package io.peekandpoke.ultra.slumber.builtin.datetime.mp

import io.peekandpoke.ultra.datetime.MpLocalTimeSlot
import io.peekandpoke.ultra.datetime.MpLocalTimeSlotRawData
import io.peekandpoke.ultra.datetime.toRawData
import io.peekandpoke.ultra.datetime.toSlotOrNull
import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Awaker for [MpLocalTimeSlot] values. Reads `{from, duration, to}` in milliseconds and decodes through
 * [toSlotOrNull]: `to` only when `duration` is absent (the old `{from, to}`). Null when `from` is missing, when
 * `duration` is present but not a number (also an explicit null), or when both `duration` and `to` are missing.
 */
object MpLocalTimeSlotAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): MpLocalTimeSlot? {

        if (data !is Map<*, *>) {
            return null
        }

        // from and to are read as MpLocalTime values (exactly one day is 24:00, every other number wraps), then handed
        // to the shared rule as milliseconds of the day
        val from = data["from"]?.let { MpLocalTimeAwaker.awake(it, context) }?.inWholeMilliSeconds() ?: return null
        val to = data["to"]?.let { MpLocalTimeAwaker.awake(it, context) }?.inWholeMilliSeconds()?.toLong()

        val raw = when {
            data.containsKey("duration") -> MpLocalTimeSlotRawData(
                from = from.toLong(),
                duration = (data["duration"] as? Number)?.takeUnless { it.isNotANumber() }?.toSaturatedLong()
                    ?: return null,
                to = to,
            )

            else -> MpLocalTimeSlotRawData(from = from.toLong(), to = to)
        }

        return raw.toSlotOrNull()
    }

    /** NaN is no duration: a decode failure, as a non-number is. */
    private fun Number.isNotANumber(): Boolean = (this is Double && isNaN()) || (this is Float && isNaN())

    /**
     * A whole number of milliseconds; beyond [Long] (a `BigInteger` or `BigDecimal` from Jackson) it saturates, by
     * comparing, never by expanding a huge exponent. The bottom is one above [Long.MIN_VALUE], which the raw shape uses
     * for "absent": any negative duration is empty. A Double or Float infinity saturates as well.
     */
    private fun Number.toSaturatedLong(): Long = when (this) {
        is BigInteger -> maxOf(minOf(this, BIG_INTEGER_MAX), BIG_INTEGER_MIN).toLong()
        is BigDecimal -> when {
            this >= BIG_DECIMAL_MAX -> Long.MAX_VALUE
            this <= BIG_DECIMAL_MIN -> Long.MIN_VALUE + 1
            else -> toLong()
        }
        else -> toLong().coerceAtLeast(Long.MIN_VALUE + 1)
    }

    private val BIG_INTEGER_MAX: BigInteger = BigInteger.valueOf(Long.MAX_VALUE)
    private val BIG_INTEGER_MIN: BigInteger = BigInteger.valueOf(Long.MIN_VALUE + 1)
    private val BIG_DECIMAL_MAX: BigDecimal = BigDecimal(Long.MAX_VALUE)
    private val BIG_DECIMAL_MIN: BigDecimal = BigDecimal(Long.MIN_VALUE + 1)
}

/** Slumberer for [MpLocalTimeSlot] values. Writes `{from, duration, to}` in milliseconds, `to` derived. */
object MpLocalTimeSlotSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any?>? {

        if (data !is MpLocalTimeSlot) {
            return null
        }

        val raw = data.toRawData()

        return mapOf("from" to raw.from, "duration" to raw.duration, "to" to raw.to)
    }
}
