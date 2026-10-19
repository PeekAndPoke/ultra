package io.peekandpoke.ultra.datetime

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.IllegalTimeZoneException

/** Fixed-offset zone ids, parsed in common code. `FixedOffsetIdJvmSpec` checks every row against java.time. */
@Suppress("unused")
class FixedOffsetIdSpec : StringSpec({

    FixedOffsetIds.offsets.forEach { (id, seconds) ->
        "'$id' is a fixed offset of $seconds seconds" {
            TimezoneEngine.fixedOffsetSecondsOrNull(id) shouldBe seconds
        }
    }

    FixedOffsetIds.regions.forEach { id ->
        "'$id' is left to the platform as a region id" {
            TimezoneEngine.fixedOffsetSecondsOrNull(id) shouldBe null
        }
    }

    FixedOffsetIds.invalid.forEach { id ->
        "'$id' is rejected" {
            shouldThrow<IllegalTimeZoneException> {
                MpTimezone.of(id).offsetSecondsAt(MpInstant.Epoch)
            }
        }
    }
})

internal object FixedOffsetIds {
    val offsets = listOf(
        "UTC" to 0,
        "GMT" to 0,
        "UT" to 0,
        "+1" to 3600,
        "-1" to -3600,
        "+01" to 3600,
        "+0130" to 5400,
        "+01:30" to 5400,
        "+013015" to 5415,
        "+01:30:15" to 5415,
        "-00:00" to 0,
        "+18:00" to 64800,
        "-18" to -64800,
        "UTC+01:00" to 3600,
        "GMT-3" to -10800,
        "UT+0130" to 5400,
        "UTC-01:30:15" to -5415,
    )

    val regions = listOf("GMT0", "Etc/UTC", "Europe/Berlin")

    val invalid = listOf(
        "+1:00", "+100", "+01:3015", "+0130:15", "+18:00:01", "+19", "+01:60", "+", "UTC+", "GMT+1:00", "UTC0",
        "europe/berlin", "constructor",
    )
}
