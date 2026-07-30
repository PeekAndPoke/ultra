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
 * Sent when a user asks to recover their account. Completing the flow behind this link proves the
 * mailbox AND invalidates every earlier password, which is why it also clears a pending activation.
 *
 * Subclass to replace it; the framework's rendering is [DefaultPasswordRecoveryEmailTemplate].
 */
abstract class PasswordRecoveryEmailTemplate :
    LocalizedEmailTemplate<PasswordRecoveryEmailTemplate.Params>() {

    data class Params(
        val recipient: EmailAddress,
        val senderEmail: String,
        val senderName: String,
        val applicationName: String,
        /** Carries a live single-use token. Never put this in a subject — subjects are not anonymized. */
        val resetUrl: String,
    )
}

/** The framework's default: minimal, unbranded, `en` + `de`. */
class DefaultPasswordRecoveryEmailTemplate(
    /** Branding chrome. Supply your own to restyle these mails without rewriting them. */
    private val layout: EmailLayout = EmailLayout.default,
) : PasswordRecoveryEmailTemplate() {

    override val fallbackLocale = Locale("en")

    override val renderers: Map<Locale, (Params, Locale) -> Email> = mapOf(
        Locale("en") to { params, locale -> renderEn(params, locale) },
        Locale("de") to { params, locale -> renderDe(params, locale) },
    )

    private fun renderEn(params: Params, locale: Locale): Email = authEmail(
        layout = layout,
        senderEmail = params.senderEmail,
        recipient = params.recipient,
        subject = "${params.applicationName}: Recover your Account",
        locale = locale,
    ) {
        h1 { +"Heads up!" }

        p {
            +"Click the link below to recover your account and set a new password."
        }

        p {
            a(href = params.resetUrl) {
                +"Recover account"
            }
        }

        authEmailSignature(locale, params.senderName)
    }

    /**
     * "Passwort zurücksetzen", NOT "Konto wiederherstellen".
     *
     * In German product language "Konto wiederherstellen" means restoring a deleted or suspended
     * account. This flow sets a new password. A user who sees only the subject line in their inbox
     * would read it as "your account was deleted" — and a security mail that misdescribes itself is
     * also the exact shape users are trained to treat as phishing.
     */
    private fun renderDe(params: Params, locale: Locale): Email = authEmail(
        layout = layout,
        senderEmail = params.senderEmail,
        recipient = params.recipient,
        subject = "${params.applicationName}: Passwort zurücksetzen",
        locale = locale,
    ) {
        h1 { +"Passwort zurücksetzen" }

        p {
            +(
                "Klicke auf den folgenden Link, um dein Konto wiederherzustellen " +
                        "und ein neues Passwort festzulegen."
                )
        }

        p {
            a(href = params.resetUrl) {
                +"Passwort zurücksetzen"
            }
        }

        authEmailSignature(locale, params.senderName)
    }
}
