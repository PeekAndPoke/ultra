package io.peekandpoke.ultra.slumber.builtin.datetime.mp

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.datetime.MpLocalTime
import io.peekandpoke.ultra.datetime.MpLocalTimeSlot
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.awake
import io.peekandpoke.ultra.slumber.slumber
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

/** ULTRA-00341 batch 3b-2: slumber stores a slot as `{from, duration, to}` and still reads the old `{from, to}`. */
class MpLocalTimeSlotCodecSpec : StringSpec({

    val codec = Codec.default
    val longest = 9_007_199_254_740_991L.milliseconds
    val night = MpLocalTimeSlot.of(MpLocalTime.of(22, 0), 4.hours)

    data class Wrapper(val slot: MpLocalTimeSlot)
    data class NullableWrapper(val slot: MpLocalTimeSlot?)

    "slumbered as {from, duration, to} in milliseconds; to is derived" {
        codec.slumber(night) shouldBe mapOf("from" to 79_200_000L, "duration" to 14_400_000L, "to" to 7_200_000L)
        codec.slumber(MpLocalTimeSlot.completeDay) shouldBe
                mapOf("from" to 0L, "duration" to 86_400_000L, "to" to 86_400_000L)
    }

    "awakes equal, also inside an object; a present duration wins over a disagreeing to" {
        codec.awake<MpLocalTimeSlot>(codec.slumber(night)) shouldBe night
        codec.awake<Wrapper>(codec.slumber(Wrapper(night))) shouldBe Wrapper(night)
        codec.awake<MpLocalTimeSlot>(mapOf("from" to 79_200_000L, "duration" to 14_400_000, "to" to 0)) shouldBe night
        codec.awake<MpLocalTimeSlot>(mapOf("from" to 79_200_000.0, "duration" to 14_400_000.0)) shouldBe night
    }

    "Duration.INFINITE is the longest slot, 2^53 - 1 ms, stored as that number with the clock time of its end" {
        val open = MpLocalTimeSlot.of(MpLocalTime.of(10, 0), Duration.INFINITE)

        codec.slumber(open) shouldBe
                mapOf("from" to 36_000_000L, "duration" to 9_007_199_254_740_991L, "to" to 68_340_991L)
        codec.awake<MpLocalTimeSlot>(codec.slumber(open)) shouldBe open
    }

    "the old shape {from, to}: after from is the difference, before from is overnight, equal is empty" {
        codec.awake<MpLocalTimeSlot>(mapOf("from" to 79_200_000L, "to" to 7_200_000L)) shouldBe night
        codec.awake<MpLocalTimeSlot>(mapOf("from" to 0L, "to" to 86_400_000L)) shouldBe MpLocalTimeSlot.completeDay
        codec.awake<MpLocalTimeSlot>(mapOf("from" to 36_000_000L, "to" to 36_000_000L))?.isEmpty shouldBe true
        codec.awake<MpLocalTimeSlot>(mapOf("from" to 86_400_000L, "to" to 86_400_000L))?.isEmpty shouldBe true
        codec.awake<MpLocalTimeSlot>(mapOf("from" to 0L, "to" to 86_399_999L))?.duration shouldBe
                86_399_999.milliseconds
    }

    "neither duration nor to, or no from, awakes to null, as an incomplete data class did" {
        codec.awake<MpLocalTimeSlot?>(mapOf("from" to 0L)) shouldBe null
        codec.awake<MpLocalTimeSlot?>(mapOf("to" to 0L)) shouldBe null
        codec.awake<MpLocalTimeSlot?>("nonsense") shouldBe null
        codec.awake<NullableWrapper>(mapOf("slot" to mapOf("from" to 0L))) shouldBe NullableWrapper(null)
    }

    "a BigDecimal with a huge exponent is compared, not expanded: fast, no exception" {
        fun awakeWithin(duration: BigDecimal): MpLocalTimeSlot? {
            val worker = Executors.newSingleThreadExecutor { Thread(it).apply { isDaemon = true } }

            return try {
                worker.submit<MpLocalTimeSlot?> {
                    codec.awake<MpLocalTimeSlot>(mapOf("from" to 0L, "duration" to duration))
                }.get(5, TimeUnit.SECONDS)
            } finally {
                worker.shutdownNow()
            }
        }

        awakeWithin(BigDecimal("1e400000000"))?.duration shouldBe longest
        awakeWithin(BigDecimal("-1e400000000"))?.isEmpty shouldBe true
        awakeWithin(BigDecimal("1e-400000000"))?.isEmpty shouldBe true
    }

    "NaN is not a duration (null); +Infinity is the longest slot, -Infinity an empty one" {
        codec.awake<MpLocalTimeSlot?>(mapOf("from" to 0L, "duration" to Double.NaN)) shouldBe null
        codec.awake<MpLocalTimeSlot?>(mapOf("from" to 0L, "duration" to Float.NaN)) shouldBe null
        codec.awake<MpLocalTimeSlot>(mapOf("from" to 0L, "duration" to Double.POSITIVE_INFINITY))?.duration shouldBe
                longest
        codec.awake<MpLocalTimeSlot>(mapOf("from" to 0L, "duration" to Double.NEGATIVE_INFINITY))?.isEmpty shouldBe true
    }

    "at the cap: 2^53 - 1 stays, 2^53 and just above are cut to it, just below stays (BigInteger, BigDecimal, Double)" {
        fun awake(duration: Number) =
            codec.awake<MpLocalTimeSlot>(mapOf("from" to 0L, "duration" to duration))?.duration

        awake(BigInteger("9007199254740991")) shouldBe longest
        awake(BigInteger("9007199254740992")) shouldBe longest
        awake(BigDecimal("9007199254740990.5")) shouldBe 9_007_199_254_740_990L.milliseconds
        awake(BigDecimal("9007199254740991.5")) shouldBe longest
        awake(9.007199254740993e15) shouldBe longest
    }

    "only an ABSENT duration falls back to to: an explicit null or a non-number duration awakes to null" {
        codec.awake<MpLocalTimeSlot?>(mapOf("from" to 0L, "duration" to null, "to" to 3_600_000L)) shouldBe null
        codec.awake<MpLocalTimeSlot?>(mapOf("from" to 0L, "duration" to "x", "to" to 3_600_000L)) shouldBe null
    }

    "a duration beyond Long (a BigInteger from Jackson) saturates to the longest slot, a negative one to empty" {
        val huge = BigInteger("18446744073709551621")

        codec.awake<MpLocalTimeSlot>(mapOf("from" to 0L, "duration" to huge))?.duration shouldBe longest
        codec.awake<MpLocalTimeSlot>(mapOf("from" to 0L, "duration" to huge.negate()))?.isEmpty shouldBe true
        codec.awake<MpLocalTimeSlot>(mapOf("from" to 0L, "duration" to BigDecimal("1e30")))?.duration shouldBe longest
        // 1e19 wraps negative through toLong(); -18446744073709551611 has +5 as its low 64 bits
        codec.awake<MpLocalTimeSlot>(mapOf("from" to 0L, "duration" to BigDecimal("1e19")))?.duration shouldBe longest
        // -1e19 has positive low 64 bits: without the lower arm it would be the longest slot
        codec.awake<MpLocalTimeSlot>(mapOf("from" to 0L, "duration" to BigDecimal("-1e19")))?.isEmpty shouldBe true
        // in range: the value itself, a fraction cut to whole milliseconds
        val decimal = BigDecimal("14400000.9")
        val integer = BigInteger.valueOf(14_400_000)
        codec.awake<MpLocalTimeSlot>(mapOf("from" to 79_200_000L, "duration" to decimal)) shouldBe night
        codec.awake<MpLocalTimeSlot>(mapOf("from" to 79_200_000L, "duration" to integer)) shouldBe night
        codec.awake<MpLocalTimeSlot>(mapOf("from" to 0L, "duration" to BigInteger("-18446744073709551611")))
            ?.isEmpty shouldBe true
        codec.awake<MpLocalTimeSlot>(mapOf("from" to 0L, "duration" to -1e30))?.isEmpty shouldBe true
        codec.awake<MpLocalTimeSlot>(mapOf("from" to 0L, "duration" to Long.MIN_VALUE))?.isEmpty shouldBe true
    }
})
