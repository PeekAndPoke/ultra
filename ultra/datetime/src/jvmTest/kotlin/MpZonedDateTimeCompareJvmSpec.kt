package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/** ULTRA-00211: with the tie-break, a sorted set keeps one instant seen in two zones as two elements. */
@Suppress("unused")
class MpZonedDateTimeCompareJvmSpec : StringSpec({

    "sortedSetOf keeps one instant in UTC and in Berlin as two elements" {
        val instant = MpInstant.parse("2022-07-01T07:00:00Z")

        sortedSetOf(instant.atZone(MpTimezone.UTC), instant.atZone(MpTimezone.of("Europe/Berlin"))).size shouldBe 2
    }
})
