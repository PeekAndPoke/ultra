package de.peekandpoke.ultra.common.datetime.kotlinx

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone

@Suppress("unused", "SpellCheckingInspection")
class KotlinxTimeZoneSpec : StringSpec({

    "Creating a timezone from an id must work" {
        TimeZone.of("Europe/Berlin").id shouldBe "Europe/Berlin"
    }

    timeZoneIds.forEach {

        "Creating a TimeZone from id '$it' must work" {
            TimeZone.of(it).id shouldBe it
        }
    }
})
