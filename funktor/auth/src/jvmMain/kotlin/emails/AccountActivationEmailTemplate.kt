package io.peekandpoke.funktor.auth.emails

import io.peekandpoke.funktor.messaging.Email
import io.peekandpoke.funktor.messaging.templates.EmailLayout
import io.peekandpoke.funktor.messaging.templates.LocalizedEmailTemplate
import io.peekandpoke.ultra.i18n.Locale
import io.peekandpoke.ultra.security.user.EmailAddress
import kotlinx.html.a
import kotlinx.html.h1
import kotlinx.html.p

/**
 * Sent at sign-up, and again when the address signs up again. Its link is where the user chooses the first
 * password, which makes this the one auth mail whose failure locks a brand-new user out entirely. It also carries the way out
 * for someone who did NOT sign up: the cancel link.
 *
 * Subclass to replace it; the framework's rendering is [DefaultAccountActivationEmailTemplate].
 */
abstract class AccountActivationEmailTemplate :
    LocalizedEmailTemplate<AccountActivationEmailTemplate.Params>() {

    /**
     * Nested so that adding a value later is a change in one file rather than a new parameter threaded
     * through every template. NOTE a renderer that ignores a new value still compiles: an app template
     * written before [cancelUrl] existed silently mails no cancel link.
     */
    data class Params(
        val recipient: EmailAddress,
        val senderEmail: String,
        val senderName: String,
        val applicationName: String,
        /**
         * Opens "Choose your password", which activates the account. Carries a live single-use token. Never put
         * this in a subject — subjects are not anonymized.
         */
        val activationUrl: String,
        /** "Didn't sign up? Cancel" — carries the account's own cancel token, live as long as the sign-up. */
        val cancelUrl: String,
    )
}

/** The framework's default: minimal, unbranded, `en` + `de`. */
class DefaultAccountActivationEmailTemplate(
    /** Branding chrome. Supply your own to restyle these mails without rewriting them. */
    private val layout: EmailLayout = EmailLayout.default,
) : AccountActivationEmailTemplate() {

    override val fallbackLocale = Locale("en")

    override val renderers: Map<Locale, (Params, Locale) -> Email> = mapOf(
        Locale("en") to { params, locale -> renderEn(params, locale) },
        Locale("de") to { params, locale -> renderDe(params, locale) },
    )

    private fun renderEn(params: Params, locale: Locale): Email = authEmail(
        layout = layout,
        senderEmail = params.senderEmail,
        recipient = params.recipient,
        subject = "${params.applicationName}: Activate your Account",
        locale = locale,
    ) {
        h1 { +"Welcome!" }

        p {
            +"Click the link below to choose your password. That activates your account."
        }

        p {
            a(href = params.activationUrl) {
                +"Choose your password"
            }
        }

        p {
            +"Didn't sign up? Someone may have used your address. "
            a(href = params.cancelUrl) {
                +"Cancel this sign-up"
            }
        }

        authEmailSignature(locale, params.senderName)
    }

    private fun renderDe(params: Params, locale: Locale): Email = authEmail(
        layout = layout,
        senderEmail = params.senderEmail,
        recipient = params.recipient,
        subject = "${params.applicationName}: Konto aktivieren",
        locale = locale,
    ) {
        h1 { +"Willkommen!" }

        p {
            +(
                "Klicke auf den folgenden Link, um dein Passwort zu wählen. " +
                        "Damit ist dein Konto aktiviert."
                )
        }

        p {
            a(href = params.activationUrl) {
                +"Passwort wählen"
            }
        }

        p {
            +"Du hast dich nicht registriert? Dann hat womöglich jemand deine Adresse benutzt. "
            a(href = params.cancelUrl) {
                +"Registrierung abbrechen"
            }
        }

        authEmailSignature(locale, params.senderName)
    }
}
