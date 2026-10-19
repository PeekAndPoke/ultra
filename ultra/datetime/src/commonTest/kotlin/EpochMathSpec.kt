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
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * [EpochMath] must agree EXACTLY with kotlinx-datetime's own (Long-based) conversion everywhere, on every target —
 * the oracle is always kotlinx's Long result, never a Double.
 */
@Suppress("unused")
class EpochMathSpec : StringSpec({

    val exactEdge = 1_000_000 * 366L * 86_400L
    val twoPow53 = 9_007_199_254_740_992L

    val spread: List<Long> = buildList {
        // ~±12 000 years around the epoch
        var x = 88_172_645_463_325_252L
        repeat(20_000) {
            x = x xor (x shl 13); x = x xor (x ushr 7); x = x xor (x shl 17)
            add(x.mod(760_000_000_000L) - 380_000_000_000L)
        }
        // around the end of the fast path
        addAll(listOf(0L, -1L, 1L, 86_399L, 86_400L, -86_400L, -86_401L, exactEdge, -exactEdge, exactEdge + 1, -exactEdge - 1))
        // Int days overflow beyond ~1.86e14 s: only the fallback gets these right
        addAll(listOf(200_000_000_000_001L, -200_000_000_000_001L))
        // beyond 2^53 a Double cannot hold every second: odd values catch any rounding on the way to kotlinx
        addAll(listOf(twoPow53 + 1, -twoPow53 - 1, 30_000_000_000_000_001L, -30_000_000_000_000_003L))
    }

    val offsets = listOf(0, 3_600, -9_000, 50_400, -43_200)

    "epoch seconds + offset -> local date-time matches kotlinx" {
        val mismatches = spread.flatMap { seconds ->
            offsets.mapNotNull { offset ->
                val nanos = seconds.mod(1_000_000_000L).toInt()
                val expected = Instant.fromEpochSeconds(seconds, nanos).plus(offset.seconds).toLocalDateTime(TimeZone.UTC)
                val actual = EpochMath.localDateTime(seconds, offset, nanos)
                if (actual != expected) "$seconds + $offset: expected $expected, got $actual" else null
            }
        }

        mismatches.take(20).shouldBeEmpty()
    }

    "local date-time - offset -> instant matches kotlinx" {
        val mismatches = spread.flatMap { seconds ->
            offsets.mapNotNull { offset ->
                val local = Instant.fromEpochSeconds(seconds, 123_456_789).toLocalDateTime(TimeZone.UTC)
                val expected = local.toInstant(TimeZone.UTC).minus(offset.seconds)
                val actual = EpochMath.instant(local, offset)
                if (actual != expected) "$local - $offset: expected $expected, got $actual" else null
            }
        }

        mismatches.take(20).shouldBeEmpty()
    }

    "calendar edge cases round-trip like kotlinx" {
        listOf(
            LocalDate(2000, 2, 29), LocalDate(1900, 2, 28), LocalDate(1900, 3, 1), LocalDate(1600, 2, 29),
            LocalDate(2100, 3, 1), LocalDate(1970, 1, 1), LocalDate(1969, 12, 31), LocalDate(0, 1, 1),
            LocalDate(0, 2, 29), LocalDate(-1, 12, 31), LocalDate(-400, 3, 1), LocalDate(-10_000, 1, 1),
            LocalDate(10_000, 1, 1), LocalDate(999_999, 12, 31), LocalDate(-999_999, 1, 1),
            LocalDate(1_000_001, 1, 1), LocalDate(300_000_000, 3, 26), LocalDate(-300_000_000, 2, 29),
        ).forEach { date ->
            withClue(date) {
                val expected = date.atStartOfDayIn(TimeZone.UTC)

                EpochMath.instant(date, 0) shouldBe expected
                EpochMath.epochSecondsExact(date) shouldBe expected.epochSeconds
                EpochMath.localDateTime(expected.epochSeconds, 0, 0).date shouldBe date
            }
        }
    }

    "a stored ts beyond 2^53 seconds survives decode and re-encode unchanged" {
        val json = Json
        // 2^53 + 1 seconds, as epoch millis
        val raw = """{"ts":9007199254740993000,"timezone":"UTC","human":""}"""

        val decoded = json.decodeFromString(MpLocalDateTime.serializer(), raw)
        val reEncoded = json.encodeToString(MpLocalDateTime.serializer(), decoded)

        reEncoded.substringAfter("\"ts\":").substringBefore(",") shouldBe "9007199254740993000"
    }
})
