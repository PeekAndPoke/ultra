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
 * Sent at sign-up, and again on resend. Until the link inside is followed the account cannot sign in,
 * which makes this the one auth mail whose failure locks a brand-new user out entirely.
 *
 * Subclass to replace it; the framework's rendering is [DefaultAccountActivationEmailTemplate].
 */
abstract class AccountActivationEmailTemplate :
    LocalizedEmailTemplate<AccountActivationEmailTemplate.Params>() {

    /**
     * Nested so that adding a value later is a change in one file rather than a new parameter
     * threaded through every template. A value added here is a compile error at each renderer that
     * has not been updated — which is the whole reason these are types and not `{{placeholders}}`.
     */
    data class Params(
        val recipient: EmailAddress,
        val senderEmail: String,
        val senderName: String,
        val applicationName: String,
        /** Carries a live single-use token. Never put this in a subject — subjects are not anonymized. */
        val activationUrl: String,
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
            +"Click the link below to activate your account. Until you do, you cannot sign in."
        }

        p {
            a(href = params.activationUrl) {
                +"Activate account"
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
                "Klicke auf den folgenden Link, um dein Konto zu aktivieren. " +
                        "Bis dahin kannst du dich nicht anmelden."
                )
        }

        p {
            a(href = params.activationUrl) {
                +"Konto aktivieren"
            }
        }

        authEmailSignature(locale, params.senderName)
    }
}
