package io.peekandpoke.ultra.tooling.i18n

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.i18n.model.LocaleCatalog

class I18nCheckerSpec : StringSpec({

    val en = YamlCatalogParser.parse(
        "en",
        """
            forms:
              a: "A {{x}}"
              b: "B"
        """.trimIndent(),
    )

    fun findings(vararg others: LocaleCatalog) = I18nChecker.check(en, others.toList())

    "base language: missing and superfluous keys are warnings" {
        val de = YamlCatalogParser.parse(
            "de",
            """
                forms:
                  a: "DE-A {{x}}"
                  c: "extra"
            """.trimIndent(),
        )
        val result = findings(de)

        result.any { it.severity == CheckSeverity.WARNING && it.key == "forms.b" } shouldBe true // missing
        result.any { it.severity == CheckSeverity.WARNING && it.key == "forms.c" } shouldBe true // superfluous
    }

    "placeholder introduced is an error; omitted is info" {
        val de = YamlCatalogParser.parse(
            "de",
            """
                forms:
                  a: "DE-A {{y}}"
                  b: "B"
            """.trimIndent(),
        )
        val result = findings(de)

        result.any { it.severity == CheckSeverity.ERROR && it.key == "forms.a" && it.message.contains("{{y}}") } shouldBe true
        result.any { it.severity == CheckSeverity.INFO && it.key == "forms.a" && it.message.contains("{{x}}") } shouldBe true
    }

    "regional variant: missing is not flagged, introduced key errors, identical value is redundant" {
        val de = YamlCatalogParser.parse(
            "de",
            """
                forms:
                  a: "DE-A {{x}}"
                  b: "DE-B"
            """.trimIndent(),
        )
        val deCH = YamlCatalogParser.parse(
            "de-CH",
            """
                forms:
                  a: "CH-A {{x}}"
                  b: "DE-B"
                  extra: "nope"
            """.trimIndent(),
        )
        val result = I18nChecker.check(en, listOf(de, deCH))

        // a dead key outside the API surface -> WARNING (same as a base language)
        result.any { it.severity == CheckSeverity.WARNING && it.locale == "de-CH" && it.key == "forms.extra" } shouldBe true
        // forms.b identical to base de -> redundant INFO
        result.any { it.severity == CheckSeverity.INFO && it.locale == "de-CH" && it.key == "forms.b" && it.message.contains("redundant") } shouldBe true
        // a regional variant is NEVER flagged for MISSING translations (it inherits its base)
        result.none { it.locale == "de-CH" && it.message.contains("missing") } shouldBe true
    }

    "a fully-translated base language produces no findings" {
        val de = YamlCatalogParser.parse(
            "de",
            """
                forms:
                  a: "DE-A {{x}}"
                  b: "DE-B"
            """.trimIndent(),
        )
        findings(de) shouldBe emptyList()
    }

    "plural placeholders are checked against the union of the fallback's forms" {
        val enPlural = YamlCatalogParser.parse(
            "en",
            """
                files_one: "one file"
                files_other: "{{count}} files in {{folder}}"
            """.trimIndent(),
        )
        val dePlural = YamlCatalogParser.parse(
            "de",
            """
                files_one: "eine Datei in {{folder}}"
                files_other: "{{count}} Dateien in {{folder}}"
            """.trimIndent(),
        )

        // {{folder}} in de's _one is in the fallback UNION (from _other) -> NOT an introduced error
        I18nChecker.check(enPlural, listOf(dePlural))
            .none { it.severity == CheckSeverity.ERROR && it.message.contains("folder") } shouldBe true
    }

    "requiredLangs: an entirely absent required locale is an error" {
        val result = I18nChecker.check(en, emptyList(), required = setOf("de"))

        result.any { it.severity == CheckSeverity.ERROR && it.locale == "de" && it.message.contains("no catalog") } shouldBe true
    }

    "requiredLangs: a missing key in a required base language is an error, not a warning" {
        val de = YamlCatalogParser.parse("de", """forms:${'\n'}  a: "DE-A {{x}}"""") // missing forms.b

        val result = I18nChecker.check(en, listOf(de), required = setOf("de"))

        result.any { it.severity == CheckSeverity.ERROR && it.key == "forms.b" } shouldBe true
    }

    "without requiredLangs a missing key is only a warning" {
        val de = YamlCatalogParser.parse("de", """forms:${'\n'}  a: "DE-A {{x}}"""")

        val result = findings(de)

        result.any { it.severity == CheckSeverity.WARNING && it.key == "forms.b" } shouldBe true
        result.none { it.severity == CheckSeverity.ERROR && it.key == "forms.b" } shouldBe true
    }

    "checkOutcome fails on any error, and on warnings only in strict mode" {
        val error = listOf(CheckFinding(CheckSeverity.ERROR, "de", "k", "x"))
        val warning = listOf(CheckFinding(CheckSeverity.WARNING, "de", "k", "x"))

        checkOutcome(error, strict = false).failed shouldBe true
        checkOutcome(warning, strict = false).failed shouldBe false
        checkOutcome(warning, strict = true).failed shouldBe true
        checkOutcome(emptyList(), strict = true).failed shouldBe false
    }
})
