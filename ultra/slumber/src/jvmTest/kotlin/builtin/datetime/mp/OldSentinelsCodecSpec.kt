package io.peekandpoke.ultra.slumber.builtin.datetime.mp

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.datetime.MpInstantRange
import io.peekandpoke.ultra.datetime.MpLocalDate
import io.peekandpoke.ultra.datetime.MpLocalDateRange
import io.peekandpoke.ultra.datetime.MpZonedDateTimeRange
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.awake

/** ULTRA-00343: ranges stored with the OLD sentinels (-10000-01-01, +10000-01-01) still read as open. */
class OldSentinelsCodecSpec : StringSpec({

    val codec = Codec.default

    val oldGenesis = mapOf("ts" to -377_736_739_200_000L, "timezone" to "UTC", "human" to "-10000-01-01T00:00:00.000Z")
    val oldDoomsday = mapOf("ts" to 253_402_300_800_000L, "timezone" to "UTC", "human" to "+10000-01-01T00:00:00.000Z")
    val stored = mapOf("from" to oldGenesis, "to" to oldDoomsday)

    "MpInstantRange, MpLocalDateRange and MpZonedDateTimeRange stored with the old sentinels have no start and no end" {
        val instants = codec.awake(MpInstantRange::class, stored)
        val dates = codec.awake(MpLocalDateRange::class, stored)
        val zoned = codec.awake(MpZonedDateTimeRange::class, stored)

        val flags = listOf(
            instants?.hasStart, instants?.hasEnd, dates?.hasStart, dates?.hasEnd, zoned?.hasStart, zoned?.hasEnd,
        )

        flags shouldBe List(6) { false }
        dates?.to shouldBe MpLocalDate.of(10000, 1, 1)
    }
})
