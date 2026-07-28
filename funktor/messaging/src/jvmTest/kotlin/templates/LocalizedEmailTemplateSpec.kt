package io.peekandpoke.funktor.messaging.templates

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.funktor.messaging.Email
import io.peekandpoke.funktor.messaging.api.EmailBody
import io.peekandpoke.funktor.messaging.api.EmailDestination
import io.peekandpoke.ultra.i18n.Locale

/**
 * The one fallback walker every email template shares.
 *
 * It is worth its own spec because a wrong answer here is invisible in production: a mail in the
 * wrong language is still a delivered, well-formed mail, and nothing downstream can tell that the
 * chain picked badly.
 */
class LocalizedEmailTemplateSpec : StringSpec({

    /** A template whose renderings are just marker subjects, so the SELECTION is what is asserted. */
    fun template(
        vararg locales: String,
        fallback: String = "en",
    ) = object : LocalizedEmailTemplate<String>() {
        override val fallbackLocale = Locale.parse(fallback)

        override val renderers: Map<Locale, (String, Locale) -> Email> = locales.associate { tag ->
            Locale.parse(tag) to { name: String, matched: Locale ->
                Email(
                    source = "s@test.com",
                    destination = EmailDestination.to("r@test.com"),
                    // The MATCHED locale is echoed into the body so tests can assert that what the
                    // walker selected is what the renderer was actually handed.
                    subject = "$tag/$name",
                    body = EmailBody.Text(matched.tag),
                )
            }
        }
    }

    fun subjectOf(t: LocalizedEmailTemplate<String>, vararg preferred: String) =
        t.render("x", preferred.map { Locale.parse(it) }).subject

    "picks an exact locale match" {
        subjectOf(template("en", "de"), "de") shouldBe "de/x"
    }

    "falls back from a region to its base language" {
        // A Swiss user must get the German mail, not the English one.
        subjectOf(template("en", "de"), "de-CH") shouldBe "de/x"
    }

    "prefers an exact regional rendering over the base language" {
        subjectOf(template("en", "de", "de-CH"), "de-CH") shouldBe "de-CH/x"
    }

    "walks the preference order, not just the first entry" {
        // THE case the previous design got wrong: the realm default is a LINK in the chain, not an
        // alternative to the user's setting. A user asking for `fr` in a German-default deployment
        // must get German, not the framework's English.
        subjectOf(template("en", "de"), "fr", "de") shouldBe "de/x"
    }

    "exhausts a preference AND its base before moving to the next" {
        subjectOf(template("en", "de", "fr"), "fr-CA", "de") shouldBe "fr/x"
    }

    "falls back to the guaranteed locale when nothing in the preference order matches" {
        subjectOf(template("en"), "fr", "es") shouldBe "en/x"
    }

    "renders the fallback when no preference is given at all" {
        subjectOf(template("en", "de")) shouldBe "en/x"
    }

    "honours a non-English fallback locale" {
        subjectOf(template("de", fallback = "de"), "fr") shouldBe "de/x"
    }

    "validate() accepts a template that can render its own fallback" {
        template("en", "de").validate()
    }

    "the renderer is handed the locale it was SELECTED for, not the one requested" {
        // Guards the one way a mixed-language mail can still ship: the map key and the locale a
        // renderer uses for its layout are two statements of the same fact. A `de-CH` request must
        // hand the `de` renderer `de` — that is what the layout declares as `lang` and what a
        // localized footer keys off.
        template("en", "de").render("x", listOf(Locale.parse("de-CH"))).body.content shouldBe "de"
        template("en", "de").render("x", listOf(Locale("fr"))).body.content shouldBe "en"
    }

    "rendererFor reports which locale a preference order resolves to" {
        template("en", "de", "de-CH").rendererFor(listOf(Locale.parse("de-CH"))).first shouldBe
                Locale.parse("de-CH")
        template("en", "de").rendererFor(listOf(Locale.parse("de-CH"))).first shouldBe Locale("de")
        template("en").rendererFor(listOf(Locale("fr"))).first shouldBe Locale("en")
    }

    "validate() accepts a regional fallback served by its base language" {
        // validate() must check the invariant rendererFor actually needs. Requiring an EXACT key
        // would reject a template declaring `de-CH` while shipping a perfectly serviceable `de`
        // rendering — a boot failure for something that works.
        template("de", fallback = "de-CH").validate()
    }

    "validate() REJECTS a template with no renderer for its fallback locale" {
        // Caught at wiring time, so it stops the app booting rather than 500-ing the first sign-up
        // by a user whose language happens to miss every rendering.
        shouldThrow<IllegalArgumentException> {
            template("de", fallback = "en").validate()
        }.message shouldContain "no renderer for it"
    }

    "render() on a template that cannot reach its fallback fails loudly rather than silently" {
        shouldThrow<IllegalStateException> {
            template("de", fallback = "en").render("x", listOf(Locale("fr")))
        }.message shouldContain "fallbackLocale"
    }
})
