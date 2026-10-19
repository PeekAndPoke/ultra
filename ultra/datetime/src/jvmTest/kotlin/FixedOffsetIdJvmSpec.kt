package io.peekandpoke.ultra.datetime

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.ZoneId

/** The rows of [FixedOffsetIds] are what java.time's `ZoneId.of` does with the same strings. */
@Suppress("unused")
class FixedOffsetIdJvmSpec : StringSpec({

    "every offset row has the offset java.time gives it" {
        FixedOffsetIds.offsets.forEach { (id, seconds) ->
            withClue(id) {
                ZoneId.of(id).rules.getOffset(Instant.EPOCH).totalSeconds shouldBe seconds
            }
        }
    }

    "every region row is a valid java.time zone id" {
        FixedOffsetIds.regions.forEach { id ->
            withClue(id) { ZoneId.of(id).id shouldBe id }
        }
    }

    "every invalid row is rejected by java.time too" {
        FixedOffsetIds.invalid.forEach { id ->
            withClue(id) { shouldThrowAny { ZoneId.of(id) } }
        }
    }
})
