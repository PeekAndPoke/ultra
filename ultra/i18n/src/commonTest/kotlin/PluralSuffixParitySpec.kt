package io.peekandpoke.ultra.i18n

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.i18n.model.normalizeLocaleTag
import io.peekandpoke.ultra.i18n.model.pluralSuffixes
import io.peekandpoke.ultra.i18n.model.splitPluralSuffix

/**
 * Pins the two facts that `model/` cannot derive from the runtime types, because it may not reference
 * them — it is source-included into buildSrc, so it must stand alone (see the contract at the top of
 * `model/I18nCatalogModel.kt`).
 *
 * Both are declared twice by necessity, so both need a test rather than a comment.
 */
class PluralSuffixParitySpec : StringSpec({

    "pluralSuffixes matches the PluralCategory enum exactly" {
        pluralSuffixes shouldBe PluralCategory.entries.map { it.suffix }.toSet()
    }

    "a category added to the enum is recognised on a key" {
        // Fails if the sets drift: every category must round-trip through the key grammar.
        PluralCategory.entries.forEach { category ->
            splitPluralSuffix("items_${category.suffix}") shouldBe ("items" to true)
        }
    }

    "normalizeLocaleTag agrees with Locale.tag for every shape" {
        listOf("de", "DE", "de-ch", "de_CH", "DE_ch", " de-ch ", "de-", "zh-Hant").forEach { raw ->
            normalizeLocaleTag(raw) shouldBe Locale.parse(raw).tag
        }
    }

    "a non-category trailing segment is not mistaken for a plural form" {
        splitPluralSuffix("some_key") shouldBe ("some_key" to false)
        splitPluralSuffix("_one") shouldBe ("_one" to false)
    }
})
