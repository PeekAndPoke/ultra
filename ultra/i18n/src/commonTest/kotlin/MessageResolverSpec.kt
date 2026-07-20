package io.peekandpoke.ultra.i18n

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MessageResolverSpec : StringSpec({

    val en = Locale("en")
    val de = Locale("de")
    val deCH = Locale("de", "CH")

    "substitutes {{placeholders}}" {
        val cat = MapI18nCatalog("en" to mapOf("greet" to "Hello {{name}}!"))
        val i18n = I18n(en, en) { install(cat) }

        i18n.resolve("greet", mapOf("name" to "Claude")) shouldBe "Hello Claude!"
    }

    "falls back from the selected language to the fallback" {
        val cat = MapI18nCatalog("en" to mapOf("x" to "EN"))
        val i18n = I18n(de, fallback = en) { install(cat) }

        i18n.resolve("x") shouldBe "EN"
    }

    "regional variant falls back de-CH -> de -> en" {
        val cat = MapI18nCatalog(
            "en" to mapOf("a" to "EN-A", "b" to "EN-B", "c" to "EN-C"),
            "de" to mapOf("a" to "DE-A", "b" to "DE-B"),
            "de-CH" to mapOf("a" to "CH-A"),
        )
        val i18n = I18n(deCH, fallback = en) { install(cat) }

        i18n.resolve("a") shouldBe "CH-A" // most specific wins
        i18n.resolve("b") shouldBe "DE-B" // inherits base German
        i18n.resolve("c") shouldBe "EN-C" // falls through to fallback
    }

    "specificity beats catalog source; same specificity: app wins" {
        val framework = MapI18nCatalog(
            "de-CH" to mapOf("k" to "FW-CH"),
            "de" to mapOf("k" to "FW-DE"),
        )
        // app only overrides base German -> framework's more specific de-CH still wins for de-CH users
        val app = MapI18nCatalog("de" to mapOf("k" to "APP-DE"))
        val i18n = I18n(deCH, fallback = en) { install(framework); install(app) }
        i18n.resolve("k") shouldBe "FW-CH"

        // app matches the specificity (de-CH) -> app (installed last) wins
        val app2 = MapI18nCatalog(
            "de-CH" to mapOf("k" to "APP-CH"),
            "de" to mapOf("k" to "APP-DE"),
        )
        val i18n2 = I18n(deCH, fallback = en) { install(framework); install(app2) }
        i18n2.resolve("k") shouldBe "APP-CH"
    }

    "substitution is single-pass: an arg value containing {{...}} is NOT re-interpreted" {
        // Regression for the cross-argument injection found in review: a de user whose display name
        // is the literal string "{{resetLink}}" must NOT cause the secret to be substituted into it.
        val cat = MapI18nCatalog("en" to mapOf("msg" to "Hi {{userName}}, token={{resetLink}}"))
        val i18n = I18n(en, en) { install(cat) }

        val out = i18n.resolve(
            "msg",
            mapOf(
                "userName" to "{{resetLink}}", // attacker-controlled, mimics the other placeholder
                "resetLink" to "SECRET",
            ),
        )

        out shouldBe "Hi {{resetLink}}, token=SECRET" // userName rendered literally, secret not leaked
    }

    "multiple distinct placeholders are all substituted" {
        val cat = MapI18nCatalog("en" to mapOf("greet" to "{{a}}-{{b}}-{{a}}"))
        val i18n = I18n(en, en) { install(cat) }

        i18n.resolve("greet", mapOf("a" to "X", "b" to "Y")) shouldBe "X-Y-X"
    }

    "an unknown placeholder is left untouched" {
        val cat = MapI18nCatalog("en" to mapOf("x" to "hi {{missing}}"))
        val i18n = I18n(en, en) { install(cat) }

        i18n.resolve("x", mapOf("other" to "z")) shouldBe "hi {{missing}}"
    }

    "a missing key returns the key itself as a visible marker" {
        val i18n = I18n(en, en) { install(MapI18nCatalog("en" to emptyMap())) }

        i18n.resolve("nope.here") shouldBe "nope.here"
    }

    "plural selects _one / _other and exposes {{count}}" {
        val cat = MapI18nCatalog(
            "en" to mapOf(
                "items_one" to "{{count}} item",
                "items_other" to "{{count}} items",
            ),
        )
        val i18n = I18n(en, en) { install(cat) }

        i18n.resolvePlural("items", 1) shouldBe "1 item"
        i18n.resolvePlural("items", 5) shouldBe "5 items"
    }

    "plural: an unknown category falls back to _other" {
        val cat = MapI18nCatalog("en" to mapOf("items_other" to "{{count}} items"))
        val i18n = I18n(en, en) { install(cat) }

        // count == 1 selects _one, which is absent -> must fall back to _other
        i18n.resolvePlural("items", 1) shouldBe "1 items"
    }

    "plural: no forms at all returns the base key as a marker" {
        val i18n = I18n(en, en) { install(MapI18nCatalog("en" to emptyMap())) }

        i18n.resolvePlural("items", 1) shouldBe "items"
    }

    "withLocale yields a new snapshot resolving in the new language" {
        val cat = MapI18nCatalog(
            "en" to mapOf("x" to "EN"),
            "de" to mapOf("x" to "DE"),
        )
        val enI18n = I18n(en, fallback = en) { install(cat) }
        val deI18n = enI18n.withLocale(de)

        enI18n.resolve("x") shouldBe "EN"
        deI18n.resolve("x") shouldBe "DE"
    }
})
