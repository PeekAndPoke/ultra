package io.peekandpoke.funktor.saas.domain

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class SlugsSpec : StringSpec({

    "normalize trims and lower-cases" {
        Slugs.normalize(" Acme ") shouldBe "acme"
        Slugs.normalize("ACME") shouldBe "acme"
        Slugs.normalize("Acme-Hotels") shouldBe "acme-hotels"
        Slugs.normalize("   ") shouldBe ""
    }

    "valid slugs pass" {
        listOf(
            "ab",
            "acme",
            "acme-hotels",
            "a1",
            "abc123",
            "x-y-z",
            "1acme",
            "a".repeat(Slugs.MAX_LENGTH),
        ).forEach { slug ->
            withClue(slug) { Slugs.validationError(slug).shouldBeNull() }
            Slugs.isValid(slug) shouldBe true
        }
    }

    "blank is rejected" {
        Slugs.validationError("").shouldNotBeNull() shouldContain "blank"
    }

    "too short is rejected" {
        Slugs.validationError("a").shouldNotBeNull() shouldContain "at least"
    }

    "too long is rejected" {
        Slugs.validationError("a".repeat(Slugs.MAX_LENGTH + 1)).shouldNotBeNull() shouldContain "at most"
    }

    "invalid characters are rejected" {
        listOf(
            "Acme",        // uppercase (normalize would fix, but the raw value must fail)
            "ab c",        // space
            "ab_c",        // underscore
            "ab.c",        // dot
            "ab/c",        // slash
            "äbc",         // non-ascii
            "ab!c",        // punctuation
        ).forEach { slug ->
            withClue(slug) {
                Slugs.validationError(slug).shouldNotBeNull() shouldContain "lowercase letters"
                Slugs.isValid(slug) shouldBe false
            }
        }
    }

    "leading or trailing hyphen is rejected" {
        Slugs.validationError("-abc").shouldNotBeNull() shouldContain "start and end"
        Slugs.validationError("abc-").shouldNotBeNull() shouldContain "start and end"
        Slugs.validationError("-").shouldNotBeNull()
    }

    "reserved names are rejected" {
        Slugs.RESERVED.forEach { reserved ->
            withClue(reserved) {
                Slugs.validationError(reserved).shouldNotBeNull() shouldContain "reserved"
            }
        }
    }
})
