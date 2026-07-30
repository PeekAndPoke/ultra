package io.peekandpoke.ultra.i18n

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class LocaleSpec : StringSpec({

    "parse: language only" {
        Locale.parse("de") shouldBe Locale("de")
    }

    "parse: language and region, normalizing case and separator" {
        Locale.parse("de-ch") shouldBe Locale("de", "CH")
        Locale.parse("DE_ch") shouldBe Locale("de", "CH")
    }

    "the constructor normalizes case too, not only parse" {
        Locale("DE") shouldBe Locale("de")
        Locale("de", "ch") shouldBe Locale("de", "CH")
        Locale("DE", "ch").tag shouldBe "de-CH"
    }

    "a blank region is dropped" {
        Locale("de", "  ") shouldBe Locale("de")
        Locale.parse("de-") shouldBe Locale("de")
    }

    "base strips the region" {
        Locale("de", "CH").base shouldBe Locale("de")
        Locale("de").base shouldBe Locale("de")
    }

    "tag renders the BCP-47 form" {
        Locale("de", "CH").tag shouldBe "de-CH"
        Locale("de").tag shouldBe "de"
    }
})
