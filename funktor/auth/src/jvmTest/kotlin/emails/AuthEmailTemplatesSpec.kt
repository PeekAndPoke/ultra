package io.peekandpoke.funktor.auth.emails

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.peekandpoke.funktor.auth.AuthRealm
import io.peekandpoke.funktor.auth.MinimalTestRealm
import io.peekandpoke.funktor.auth.MinimalTestUser
import io.peekandpoke.funktor.auth.model.AuthUser
import io.peekandpoke.funktor.auth.model.LanguageSettings
import io.peekandpoke.funktor.messaging.Email
import io.peekandpoke.funktor.messaging.api.EmailBody
import io.peekandpoke.funktor.messaging.api.EmailDestination
import io.peekandpoke.funktor.messaging.senders.hrefs
import io.peekandpoke.funktor.messaging.templates.EmailLayout
import io.peekandpoke.ultra.i18n.Locale
import io.peekandpoke.ultra.security.user.EmailAddress
import kotlinx.html.body
import kotlinx.html.div

/**
 * What the three framework auth mails actually say, in both languages.
 *
 * The English strings are asserted verbatim because two e2e specs match on them; the German ones
 * because nothing else in the repo would notice if a rendering silently fell back to English.
 */
class AuthEmailTemplatesSpec : StringSpec({

    val recipient = EmailAddress.of("user@test.com")
    val en = listOf(Locale("en"))
    val de = listOf(Locale("de"))

    val templates = AuthEmailTemplates()

    fun activation(preferred: List<Locale>) = templates.accountActivation.render(
        params = AccountActivationEmailTemplate.Params(
            recipient = recipient,
            senderEmail = "sender@test.com",
            senderName = "Sender Name",
            applicationName = "Demo App",
            activationUrl = "https://app.test/auth/email-password/activate/S3cr3t%2BT0ken",
        ),
        preferred = preferred,
    )

    fun recovery(preferred: List<Locale>) = templates.passwordRecovery.render(
        params = PasswordRecoveryEmailTemplate.Params(
            recipient = recipient,
            senderEmail = "sender@test.com",
            senderName = "Sender Name",
            applicationName = "Demo App",
            resetUrl = "https://app.test/auth/email-password/reset-password/S3cr3t%2BT0ken",
        ),
        preferred = preferred,
    )

    fun passwordChanged(preferred: List<Locale>) = templates.passwordChanged.render(
        params = PasswordChangedEmailTemplate.Params(
            recipient = recipient,
            senderEmail = "sender@test.com",
            senderName = "Sender Name",
            applicationName = "Demo App",
        ),
        preferred = preferred,
    )

    "AuthEmailTemplates REJECTS a template that cannot render its own fallback locale" {
        // The constructor is the wiring point, so a broken template must fail HERE — at boot —
        // rather than 500 the first sign-up that reaches it.
        //
        // Asserted with a deliberately broken template, not by constructing the healthy default:
        // the latter passes whether or not `validate()` is ever called, which would leave this
        // reading as though it proved something.
        val broken = object : AccountActivationEmailTemplate() {
            override val fallbackLocale = Locale("en")
            override val renderers = emptyMap<Locale, (Params, Locale) -> Email>()
        }

        shouldThrow<IllegalArgumentException> {
            AuthEmailTemplates(accountActivation = broken)
        }.message shouldContain "no renderer for it"
    }

    "the locale preference puts the realm default BETWEEN the user's language and the fallback" {
        // THE rule the previous design got wrong, and the reason this is a list. A user asking for a
        // language nobody has translated must land on the realm's default, not skip past it to `en`.
        val frenchUser = object : AuthUser {
            override val email = recipient
            override val language = LanguageSettings(messaging = "fr")
        }

        authEmailLocales(frenchUser, realmDefault = Locale("de")) shouldBe
                listOf(Locale("fr"), Locale("de"))

        // ... and that preference actually selects the German rendering.
        activation(authEmailLocales(frenchUser, realmDefault = Locale("de"))).subject shouldBe
                "Demo App: Konto aktivieren"
    }

    "a user with no language of their own gets the realm default" {
        val quietUser = object : AuthUser {
            override val email = recipient
        }

        authEmailLocales(quietUser, realmDefault = Locale("de")) shouldBe listOf(Locale("de"))
    }

    "a stored language tag is normalized before it is used" {
        // `"DE_ch"` is the shape a careless profile writer stores. Without `Locale.parse` it matches
        // no rendering at all and the user silently gets English.
        val user = object : AuthUser {
            override val email = recipient
            override val language = LanguageSettings(messaging = "DE_ch")
        }

        authEmailLocales(user, realmDefault = Locale("en")) shouldBe
                listOf(Locale.parse("de-CH"), Locale("en"))

        activation(authEmailLocales(user, realmDefault = Locale("en"))).subject shouldBe
                "Demo App: Konto aktivieren"
    }

    "a blank language setting is ignored rather than becoming a junk locale" {
        val user = object : AuthUser {
            override val email = recipient
            override val language = LanguageSettings(messaging = "   ")
        }

        authEmailLocales(user, realmDefault = Locale("de")) shouldBe listOf(Locale("de"))
    }

    "a custom layout reaches ALL THREE mails, and sees the locale" {
        // Branding is the common reason to touch these mails, so `default(layout)` exists to hand one
        // layout to all three. The failure it guards against is partial: an app that wires two of the
        // three by hand and silently ships the framework's unbranded chrome on the one it forgot.
        //
        // The locale assertion is not decoration either — a footer carries the imprint and the
        // unsubscribe line, and a layout that cannot see the locale can only put an English one under
        // a German body.
        val branded = EmailLayout { locale, content ->
            EmailBody.Html {
                body {
                    div { +"BRAND-HEADER" }
                    content()
                    div { +"FOOTER-${locale.tag}" }
                }
            }
        }

        val custom = AuthEmailTemplates.default(branded)

        val mails = listOf(
            custom.accountActivation.render(
                AccountActivationEmailTemplate.Params(
                    recipient = recipient,
                    senderEmail = "s@test.com",
                    senderName = "S",
                    applicationName = "A",
                    activationUrl = "https://app.test/a",
                ),
                de,
            ),
            custom.passwordChanged.render(
                PasswordChangedEmailTemplate.Params(
                    recipient = recipient,
                    senderEmail = "s@test.com",
                    senderName = "S",
                    applicationName = "A",
                ),
                de,
            ),
            custom.passwordRecovery.render(
                PasswordRecoveryEmailTemplate.Params(
                    recipient = recipient,
                    senderEmail = "s@test.com",
                    senderName = "S",
                    applicationName = "A",
                    resetUrl = "https://app.test/r",
                ),
                de,
            ),
        )

        mails.forEach { mail ->
            withClue(mail.subject) {
                mail.body.content shouldContain "BRAND-HEADER"
                mail.body.content shouldContain "FOOTER-de"
            }
        }
    }

    "the activation mail addresses the recipient and comes from the configured sender" {
        val mail = activation(en)

        mail.source shouldBe "sender@test.com"
        mail.destination.toAddresses shouldBe listOf("user@test.com")
    }

    "the activation mail in English" {
        val mail = activation(en)

        mail.subject shouldBe "Demo App: Activate your Account"
        mail.body.content shouldContain "Welcome!"
        mail.body.content shouldContain "Click the link below to activate your account"
        mail.body.content shouldContain "Activate account"
    }

    "the activation mail in German, with nothing English left in it" {
        val mail = activation(de)

        mail.subject shouldBe "Demo App: Konto aktivieren"
        mail.body.content shouldContain "Willkommen!"
        mail.body.content shouldContain "Konto aktivieren"

        // Whole-email selection: a subject from one language and a body from another is not a worse
        // translation, it is a broken one.
        mail.body.content shouldNotContain "Welcome!"
        mail.body.content shouldNotContain "Activate account"
    }

    "non-ASCII survives into the rendered body" {
        // A Kotlin string literal through kotlinx.html into EmailBody.Html. Cheap to assert and the
        // only thing that catches an encoding fault between source file and sent mail.
        activation(de).body.content shouldContain "Viele Grüße"
    }

    "the rendered document declares a doctype, its language and its charset" {
        val mail = activation(de)

        // The doctype is what the charset actually depends on: every standalone render path parses
        // in quirks mode without it, which is where the encoding guess goes wrong.
        mail.body.content shouldContain "<!DOCTYPE html>"
        mail.body.content shouldContain """lang="de""""
        mail.body.content shouldContain "charset=\"utf-8\""
    }

    "the declared lang follows the MATCHED locale, not the requested one" {
        // `de-CH` resolves to the `de` rendering, so the document must say `de`. If the renderer
        // named its own locale instead of being handed the matched one, this is where a French
        // rendering copied from the German one would ship as `lang="de"`.
        activation(listOf(Locale.parse("de-CH"))).body.content shouldContain """lang="de""""
        activation(listOf(Locale("fr"))).body.content shouldContain """lang="en""""
    }

    "branding a subset still leaves the rest branded" {
        // The combination that used to be impossible: override ONE template and brand the other two.
        // Before `layout` moved onto the constructor, an app doing both silently shipped framework
        // chrome on the two it did not name.
        val branded = EmailLayout { _, content -> EmailBody.Html { body { div { +"BRAND" }; content() } } }

        val custom = AuthEmailTemplates(
            layout = branded,
            accountActivation = DefaultAccountActivationEmailTemplate(branded),
        )

        custom.passwordChanged.render(
            PasswordChangedEmailTemplate.Params(
                recipient = recipient,
                senderEmail = "s@test.com",
                senderName = "S",
                applicationName = "A",
            ),
            en,
        ).body.content shouldContain "BRAND"
    }

    "every auth mail carries the sender name" {
        // Not decoration: in the previous placeholder-based design this value could be dropped from
        // the args map and ship a literal `{{senderName}}` to real recipients with the whole suite
        // green. It is a constructor parameter now, but the assertion is still what proves it is
        // actually rendered rather than merely accepted.
        listOf(activation(en), recovery(en), passwordChanged(en), activation(de)).forEach { mail ->
            withClue(mail.subject) { mail.body.content shouldContain "Sender Name" }
        }
    }

    "a link-carrying mail carries EXACTLY one link, and it survives rendering intact" {
        // `hrefs()` is what the e2e specs use to pull the token back out. A base64 token contains
        // `+` and `=` routinely, so a rendering that mangles it produces a dead link while every
        // server-side test still passes.
        listOf(
            activation(en) to "https://app.test/auth/email-password/activate/S3cr3t%2BT0ken",
            activation(de) to "https://app.test/auth/email-password/activate/S3cr3t%2BT0ken",
            recovery(en) to "https://app.test/auth/email-password/reset-password/S3cr3t%2BT0ken",
            recovery(de) to "https://app.test/auth/email-password/reset-password/S3cr3t%2BT0ken",
        ).forEach { (mail, expected) ->
            withClue(mail.subject) {
                mail.hrefs() shouldHaveSize 1
                mail.hrefs().single() shouldBe expected
            }
        }
    }

    "the password-changed mail carries NO link" {
        // It exists to make an unexpected change visible; a link in it is a phishing template.
        passwordChanged(en).hrefs() shouldBe emptyList()
    }

    "the password-changed and recovery mails in German" {
        passwordChanged(de).subject shouldBe "Demo App: Dein Passwort wurde geändert"
        recovery(de).subject shouldBe "Demo App: Passwort zurücksetzen"
        recovery(de).body.content shouldContain "Passwort zurücksetzen"
    }

    "an untranslated language falls back to English" {
        activation(listOf(Locale("fr"))).subject shouldBe "Demo App: Activate your Account"
    }

    "a regional variant resolves to its base language" {
        activation(listOf(Locale.parse("de-CH"))).subject shouldBe "Demo App: Konto aktivieren"
    }
})

/**
 * The WIRING between the realm and the templates, which `AuthEmailTemplatesSpec` cannot see: it tests
 * the free `authEmailLocales` rule directly, so a `DefaultMessaging` that ignored the realm and passed
 * a hardcoded `en` would leave that whole spec green.
 *
 * `AuthRealm.defaultLanguage` is new public API whose only consumer is `DefaultMessaging`. A German
 * deployment setting it and still receiving framework English is exactly the failure this redesign was
 * justified on, so it gets a test of its own.
 */
class DefaultMessagingLocaleWiringSpec : StringSpec({

    fun messagingFor(realmDefault: Locale) = AuthRealm.DefaultMessaging(
        senderEmail = "sender@test.com",
        senderName = "Sender",
        applicationName = "Demo App",
        realm = MinimalTestRealm(defaultLanguage = realmDefault),
    )

    "DefaultMessaging consults the REALM's default language, not a hardcoded 'en'" {
        val quietUser = MinimalTestUser()

        messagingFor(Locale("de")).preferredLocales(quietUser) shouldBe listOf(Locale("de"))
    }

    "the user's own language still outranks the realm default" {
        val germanUser = MinimalTestUser(language = LanguageSettings(messaging = "fr"))

        messagingFor(Locale("de")).preferredLocales(germanUser) shouldBe
                listOf(Locale("fr"), Locale("de"))
    }
})

/**
 * The envelope invariant. A template builds its own `Email`, so nothing but this stops an
 * app-supplied one from mailing a live activation token somewhere else.
 */
class AuthEmailEnvelopeSpec : StringSpec({

    val user = EmailAddress.of("user@test.com")

    fun mailTo(destination: EmailDestination) = Email(
        source = "sender@test.com",
        destination = destination,
        subject = "s",
        body = EmailBody.Text("b"),
    )

    "accepts a mail addressed to exactly the user" {
        requireAuthEmailEnvelope(mailTo(EmailDestination.to(user.value)), user)
    }

    "REJECTS a mail addressed to somebody else" {
        shouldThrow<IllegalArgumentException> {
            requireAuthEmailEnvelope(mailTo(EmailDestination.to("attacker@evil.test")), user)
        }.message shouldContain "addressed to exactly the user"
    }

    "REJECTS a cc — the token would reach a second mailbox" {
        // The realistic slip: an app template ccing a shared support address "so onboarding is
        // visible". Every activation link in that inbox is a working credential.
        shouldThrow<IllegalArgumentException> {
            requireAuthEmailEnvelope(
                mailTo(EmailDestination(toAddresses = listOf(user.value), ccAddresses = listOf("support@app.test"))),
                user,
            )
        }
    }

    "REJECTS a bcc, which is the same leak without the evidence" {
        shouldThrow<IllegalArgumentException> {
            requireAuthEmailEnvelope(
                mailTo(EmailDestination(toAddresses = listOf(user.value), bccAddresses = listOf("x@evil.test"))),
                user,
            )
        }
    }

    "REJECTS a second To address" {
        shouldThrow<IllegalArgumentException> {
            requireAuthEmailEnvelope(
                mailTo(EmailDestination(toAddresses = listOf(user.value, "x@evil.test"))),
                user,
            )
        }
    }
})
