package io.peekandpoke.funktor.auth.emails

import io.peekandpoke.funktor.messaging.Email
import io.peekandpoke.funktor.messaging.templates.EmailLayout
import io.peekandpoke.funktor.messaging.templates.LocalizedEmailTemplate
import io.peekandpoke.ultra.i18n.Locale
import io.peekandpoke.ultra.security.user.EmailAddress
import kotlinx.html.h1
import kotlinx.html.p

/**
 * Sent after a password change. Carries no link — it exists so that a change the user did NOT make is
 * visible to them, which is the only thing that turns a silent account takeover into a reported one.
 *
 * Subclass to replace it; the framework's rendering is [DefaultPasswordChangedEmailTemplate].
 */
abstract class PasswordChangedEmailTemplate :
    LocalizedEmailTemplate<PasswordChangedEmailTemplate.Params>() {

    data class Params(
        val recipient: EmailAddress,
        val senderEmail: String,
        val senderName: String,
        val applicationName: String,
    )
}

/** The framework's default: minimal, unbranded, `en` + `de`. */
class DefaultPasswordChangedEmailTemplate(
    /** Branding chrome. Supply your own to restyle these mails without rewriting them. */
    private val layout: EmailLayout = EmailLayout.default,
) : PasswordChangedEmailTemplate() {

    override val fallbackLocale = Locale("en")

    override val renderers: Map<Locale, (Params, Locale) -> Email> = mapOf(
        Locale("en") to { params, locale -> renderEn(params, locale) },
        Locale("de") to { params, locale -> renderDe(params, locale) },
    )

    private fun renderEn(params: Params, locale: Locale): Email = authEmail(
        layout = layout,
        senderEmail = params.senderEmail,
        recipient = params.recipient,
        subject = "${params.applicationName}: Your password was changed",
        locale = locale,
    ) {
        h1 { +"Heads up!" }

        p {
            +"Your password was changed. If this was not you, please contact us!"
        }

        authEmailSignature(locale, params.senderName)
    }

    /** "Achtung!" is right HERE, unlike on the recovery mail — this one is a genuine alert. */
    private fun renderDe(params: Params, locale: Locale): Email = authEmail(
        layout = layout,
        senderEmail = params.senderEmail,
        recipient = params.recipient,
        subject = "${params.applicationName}: Dein Passwort wurde geändert",
        locale = locale,
    ) {
        h1 { +"Achtung!" }

        p {
            +"Dein Passwort wurde geändert. Falls du das nicht warst, kontaktiere uns bitte!"
        }

        authEmailSignature(locale, params.senderName)
    }
}
