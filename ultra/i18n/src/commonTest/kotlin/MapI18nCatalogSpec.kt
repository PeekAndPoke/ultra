package io.peekandpoke.ultra.i18n

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MapI18nCatalogSpec : StringSpec({

    "returns the template for an exactly matching locale tag" {
        val cat = MapI18nCatalog("de-CH" to mapOf("k" to "V"))

        cat.template("k", Locale("de", "CH")) shouldBe "V"
    }

    "does NOT fall back across locales — that is the resolver's job" {
        val cat = MapI18nCatalog("de" to mapOf("k" to "V"))

        cat.template("k", Locale("de", "CH")) shouldBe null // base German is not returned for de-CH here
    }

    "returns null for a missing key" {
        val cat = MapI18nCatalog("de" to mapOf("k" to "V"))

        cat.template("other", Locale("de")) shouldBe null
    }
})
