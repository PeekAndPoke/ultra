package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

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
    }
})
