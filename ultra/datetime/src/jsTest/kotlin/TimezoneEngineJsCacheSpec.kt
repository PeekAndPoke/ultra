package io.peekandpoke.ultra.datetime

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlinx.datetime.IllegalTimeZoneException
import kotlin.time.Duration.Companion.days

@Suppress("unused")
class TimezoneEngineJsCacheSpec : StringSpec({

    val instant = MpInstant.fromEpochMillis(1_700_000_000_000)

    "a supported zone id is cached, one newer than the list of 2021 included" {
        instant.atZone(MpTimezone.of("Pacific/Kanton"))

        cachedZoneIds() shouldContain "Pacific/Kanton"
    }

    // ULTRA-00217: Intl matches ids case-insensitively, and the case check covers listed ids only
    "case variants of an unlisted zone id resolve, but are not cached" {
        val unlisted = "Etc/UCT"
        MpTimezone.supportedIds.toList() shouldNotContain unlisted

        val expected = instant.atZone(MpTimezone.UTC).datetime
        val before = cachedZoneIds()

        // every upper/lower case combination of the six letters
        val letters = unlisted.indices.filter { unlisted[it].isLetter() }
        val variants = (0 until (1 shl letters.size)).map { mask ->
            unlisted.mapIndexed { index, char ->
                val bit = letters.indexOf(index)
                if (bit >= 0 && ((mask shr bit) and 1) == 1) char.uppercaseChar() else char.lowercaseChar()
            }.joinToString("")
        }

        variants.size shouldBe 64

        variants.forEach { id ->
            instant.atZone(MpTimezone.of(id)).datetime shouldBe expected
        }

        cachedZoneIds() shouldBe before
        memoizedUnlistedZoneId() shouldBe variants.last()
    }

    // ULTRA-00217: one conversion asks several times; an unlisted id keeps one memo slot instead of a new formatter each
    "repeated lookups of one unlisted id reuse its memo slot" {
        val unlisted = MpTimezone.of("Etc/UCT")
        // another unlisted id takes the slot first
        instant.atZone(MpTimezone.of("ETC/UCT"))

        val before = intlFormattersBuilt

        repeat(10) { day ->
            val zoned = instant.plus(day.days).atZone(unlisted)
            zoned.toLocalDate().atStartOfDay(unlisted)
        }

        intlFormattersBuilt - before shouldBe 1
        memoizedUnlistedZoneId() shouldBe "Etc/UCT"
    }

    "an unknown id and a case variant of a listed one are refused with quoted ids; failed builds are not counted" {
        val before = intlFormattersBuilt

        shouldThrow<IllegalTimeZoneException> {
            instant.atZone(MpTimezone.of("Mars/Olympus_Mons"))
        }.message shouldBe "Unknown time zone: 'Mars/Olympus_Mons'"

        shouldThrow<IllegalTimeZoneException> {
            instant.atZone(MpTimezone.of("europe/berlin"))
        }.message shouldBe "Unknown time zone: 'europe/berlin' (ids are case-sensitive: Europe/Berlin)"

        intlFormattersBuilt shouldBe before
    }
})
