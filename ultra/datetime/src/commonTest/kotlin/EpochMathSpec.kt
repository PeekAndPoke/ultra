package io.peekandpoke.ultra.datetime

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** [EpochMath] must agree with kotlinx-datetime's own (Long-based) conversion everywhere, on every target. */
@Suppress("unused")
class EpochMathSpec : StringSpec({

    // ~±12 000 years around the epoch, plus the edges of EpochMath's exact range and beyond it (kotlinx fallback)
    val spread: List<Long> = buildList {
        var x = 88_172_645_463_325_252L
        repeat(20_000) {
            x = x xor (x shl 13); x = x xor (x ushr 7); x = x xor (x shl 17)
            add(x.mod(760_000_000_000L) - 380_000_000_000L)
        }
        val exactEdge = (1_000_000 * 366L * 86_400L)
        addAll(listOf(0L, -1L, 1L, 86_399L, 86_400L, -86_400L, -86_401L, exactEdge, -exactEdge, exactEdge + 1, -exactEdge - 1))
        // far beyond: epoch days no longer fit an Int, only the kotlinx fallback gets these right
        addAll(listOf(40_000_000_000_000L, -40_000_000_000_000L, 30_000_000_000_000_000L, -30_000_000_000_000_000L))
    }

    "epoch seconds -> local date-time matches kotlinx" {
        val mismatches = spread.mapNotNull { seconds ->
            val nanos = (seconds.mod(1_000_000_000L)).toInt()
            val expected = Instant.fromEpochSeconds(seconds, nanos).toLocalDateTime(TimeZone.UTC)
            val actual = EpochMath.localDateTime(seconds.toDouble(), nanos)
            if (actual != expected) "$seconds: expected $expected, got $actual" else null
        }

        mismatches.take(20).shouldBeEmpty()
    }

    "local date-time -> epoch seconds matches kotlinx" {
        val mismatches = spread.mapNotNull { seconds ->
            val local = Instant.fromEpochSeconds(seconds).toLocalDateTime(TimeZone.UTC)
            val expected = local.toInstant(TimeZone.UTC).epochSeconds.toDouble()
            val actual = EpochMath.epochSeconds(local)
            if (actual != expected) "$local: expected $expected, got $actual" else null
        }

        mismatches.take(20).shouldBeEmpty()
    }

    "calendar edge cases round-trip like kotlinx" {
        listOf(
            LocalDate(2000, 2, 29), LocalDate(1900, 2, 28), LocalDate(1900, 3, 1), LocalDate(1600, 2, 29),
            LocalDate(2100, 3, 1), LocalDate(1970, 1, 1), LocalDate(1969, 12, 31), LocalDate(0, 1, 1),
            LocalDate(0, 2, 29), LocalDate(-1, 12, 31), LocalDate(-400, 3, 1), LocalDate(-10_000, 1, 1),
            LocalDate(10_000, 1, 1), LocalDate(999_999, 12, 31), LocalDate(-999_999, 1, 1),
        ).forEach { date ->
            withClue(date) {
                val expected = date.atStartOfDayIn(TimeZone.UTC).epochSeconds.toDouble()

                EpochMath.epochSeconds(date) shouldBe expected
                EpochMath.localDateTime(expected, 0).date shouldBe date
            }
        }
    }
})
