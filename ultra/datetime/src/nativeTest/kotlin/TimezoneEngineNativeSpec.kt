package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class TimezoneEngineNativeSpec : StringSpec({

    val instant = MpInstant.fromEpochMillis(1_700_000_000_000)

    "a supported zone id is cached" {
        instant.atZone(MpTimezone.of("Europe/Berlin"))

        cachedZoneIds() shouldContain "Europe/Berlin"
    }

    // ULTRA-00217: kotlinx/native resolves path variants of an id, each one used to become a permanent cache entry
    "path variants of a zone id still resolve, but are not cached" {
        val expected = instant.atZone(MpTimezone.of("Europe/Berlin")).toIsoString()
        val before = cachedZoneIds()

        val variants = listOf("Europe//Berlin", "Europe/Berlin/", "./Europe/Berlin") +
                (1..200).map { n -> "Europe/" + "./".repeat(n) + "Berlin" }

        variants.forEach { id ->
            instant.atZone(MpTimezone.of(id)).toIsoString() shouldBe expected.replace("[Europe/Berlin]", "[$id]")
        }

        cachedZoneIds() shouldBe before
    }
})
