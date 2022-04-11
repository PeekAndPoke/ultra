package de.peekandpoke.ultra.common.datetime.kotlinx

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone

@Suppress("unused")
class KotlinxTimeZoneSpec : StringSpec({
    "Creating a timezone from an id must work" {
        TimeZone.of("Europe/Berlin").id shouldBe "Europe/Berlin"
    }
})
