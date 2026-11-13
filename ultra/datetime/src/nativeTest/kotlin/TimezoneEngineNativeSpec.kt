package io.peekandpoke.ultra.datetime

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlinx.datetime.IllegalTimeZoneException
import kotlin.time.Duration.Companion.days

class TimezoneEngineNativeSpec : StringSpec({

    val instant = MpInstant.fromEpochMillis(1_700_000_000_000)

    "a supported zone id is cached" {
        instant.atZone(MpTimezone.of("Europe/Berlin"))

        cachedZoneIds() shouldContain "Europe/Berlin"
    }

    // ULTRA-00217: canonical ids newer than the list of 2021 must be cached too
    listOf(
        "America/Ciudad_Juarez", "America/Coyhaique", "Europe/Kyiv", "Pacific/Kanton",
    ).forEach { id ->
        "the zone id $id is supported and cached" {
            instant.atZone(MpTimezone.of(id))

            cachedZoneIds() shouldContain id
        }
    }

    // ULTRA-00217: an unlisted id that resolves keeps one memo slot, so the caches stay bounded
    "a flood of unlisted ids leaves the cache unchanged, the memo slot holds only the last" {
        val before = cachedZoneIds()
        val unlisted = listOf("EST", "HST", "MST", "ROC", "Etc/UCT")

        unlisted.forEach { id -> instant.atZone(MpTimezone.of(id)) }

        cachedZoneIds() shouldBe before
        memoizedUnlistedZoneId() shouldBe "Etc/UCT"
    }

    "repeated lookups of one unlisted id reuse its memo slot" {
        val unlisted = MpTimezone.of("HST")
        // another unlisted id takes the slot first
        instant.atZone(MpTimezone.of("EST"))

        val before = zoneFileReads()

        repeat(10) { day ->
            val zoned = instant.plus(day.days).atZone(unlisted)
            zoned.toLocalDate().atStartOfDay(unlisted)
        }

        zoneFileReads() - before shouldBe 1
        memoizedUnlistedZoneId() shouldBe "HST"
    }

    // ULTRA-00217 / ULTRA-00317: kotlinx/native resolves path variants of an id; ultra refuses them before the lookup
    "path variants of a zone id are refused and not cached" {
        val before = cachedZoneIds()

        val variants = listOf("Europe//Berlin", "Europe/Berlin/", "./Europe/Berlin") +
                (1..20).map { n -> "Europe/" + "./".repeat(n) + "Berlin" }

        variants.forEach { id ->
            shouldThrow<IllegalTimeZoneException> { instant.atZone(MpTimezone.of(id)) }
        }

        cachedZoneIds() shouldBe before
    }
})
