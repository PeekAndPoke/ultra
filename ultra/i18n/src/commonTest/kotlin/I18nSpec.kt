package io.peekandpoke.ultra.i18n

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class I18nSpec : StringSpec({

    val en = Locale("en")
    val de = Locale("de")

    "translate exposes the same I18n instance (the codegen seam)" {
        val i18n = I18n(en, en)
        i18n.translate.i18n shouldBe i18n
    }

    "format exposes the current locale" {
        val i18n = I18n(Locale("de", "CH"), fallback = en)
        i18n.format.locale shouldBe Locale("de", "CH")
    }

    "the fallback is kept verbatim, never derived from the locale (D4 safety net)" {
        // A regional locale must still reach the explicit fallback at the end of the chain.
        val i18n = I18n(Locale("de", "CH"), fallback = en)
        i18n.fallback shouldBe en
    }

    "withLocale preserves fallback and catalogs" {
        val cat = MapI18nCatalog(
            "en" to mapOf("x" to "EN"),
            "de" to mapOf("x" to "DE"),
        )
        val base = I18n(en, fallback = en) { install(cat) }
        val switched = base.withLocale(de)

        switched.locale shouldBe de
        switched.fallback shouldBe en       // preserved, not re-derived from the new locale
        switched.resolve("x") shouldBe "DE" // catalogs carried over
    }
})
