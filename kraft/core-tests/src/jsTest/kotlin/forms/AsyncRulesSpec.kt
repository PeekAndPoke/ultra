package io.peekandpoke.kraft.coretests.forms

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.forms.validation.anyRuleOf
import io.peekandpoke.kraft.forms.validation.given
import io.peekandpoke.kraft.forms.validation.nonNullAnd
import io.peekandpoke.kraft.forms.validation.strings.notBlank
import io.peekandpoke.kraft.i18n.installKraftForms
import io.peekandpoke.ultra.i18n.I18n
import io.peekandpoke.ultra.i18n.Locale
import io.peekandpoke.ultra.i18n.MapI18nCatalog
import kotlinx.coroutines.delay

/**
 * Covers the new async (`suspend`) rule contract — rules that validate against an async source, e.g.
 * asking a server "is this slug still available?". [delay] here stands in for a network round-trip.
 */
class AsyncRulesSpec : StringSpec({

    // Simulates a server round-trip checking slug availability (a blank slug is never available).
    val taken = setOf("admin", "root")
    suspend fun isSlugAvailable(slug: String): Boolean {
        delay(1)
        return slug.isNotBlank() && slug !in taken
    }

    "given() with a suspend check validates against an async source (slug availability)" {
        val rule = given<String>({ isSlugAvailable(it) }) { "This slug is already taken" }

        rule.check("my-shop") shouldBe true
        rule.check("admin") shouldBe false
        rule.getMessage("admin") shouldBe "This slug is already taken"
    }

    "given() async + i18n message resolves via a supplied catalog in both languages" {
        val custom = MapI18nCatalog(
            "en" to mapOf("app.taken" to "Taken"),
            "de" to mapOf("app.taken" to "Vergeben"),
        )
        fun tFor(lang: String) = I18n(Locale.parse(lang), fallback = Locale("en")) {
            installKraftForms(); install(custom)
        }.translate

        val rule = given<String>({ isSlugAvailable(it) }) { _, t -> t.i18n.resolve("app.taken") }

        rule.check("admin") shouldBe false
        rule.getMessage("admin", tFor("en")) shouldBe "Taken"
        rule.getMessage("admin", tFor("de")) shouldBe "Vergeben"
    }

    "anyRuleOf composes a sync child and an async child (check + message)" {
        val rule = anyRuleOf(
            notBlank<String>(),
            given({ isSlugAvailable(it) }) { "already taken" },
        )

        rule.check("my-shop") shouldBe true   // notBlank passes
        rule.check("admin") shouldBe true      // notBlank passes even though the async child fails
        rule.check("") shouldBe false          // both fail

        // both children failed → message lists both, joined with the default "or"
        rule.getMessage("") shouldBe "Must not be blank or already taken"
    }

    "nonNullAnd delegates to an async inner rule" {
        val rule = nonNullAnd(given<String>({ isSlugAvailable(it) }) { "already taken" })

        rule.check(null) shouldBe false
        rule.check("my-shop") shouldBe true
        rule.check("admin") shouldBe false

        rule.getMessage("admin") shouldBe "already taken"
        rule.getMessage(null) shouldBe "Must not be empty"
    }
})
