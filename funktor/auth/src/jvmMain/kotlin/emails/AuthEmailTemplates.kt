package io.peekandpoke.funktor.auth.emails

import io.peekandpoke.funktor.auth.model.AuthUser
import io.peekandpoke.funktor.messaging.Email
import io.peekandpoke.funktor.messaging.api.EmailDestination
import io.peekandpoke.funktor.messaging.templates.EmailLayout
import io.peekandpoke.ultra.i18n.Locale
import io.peekandpoke.ultra.security.user.EmailAddress
import kotlinx.html.BODY
import kotlinx.html.br
import kotlinx.html.p

/**
 * The templates behind the auth flow's transactional emails.
 *
 * Held as NAMED PROPERTIES rather than looked up by a string key: `templates.accountActivation` either
 * exists or does not compile, whereas a lookup by name can only fail at send time — and the send time
 * of an activation mail is the moment a user is left with an account they cannot get into.
 *
 * An app overrides a mail by supplying its own subclass of the relevant template. It replaces the
 * whole message, in every locale it declares, which is the only kind of override that cannot produce
 * half-branded output.
 */
class AuthEmailTemplates(
    /**
     * Branding chrome for the templates that are left at their framework default.
     *
     * FIRST parameter so that the common case — brand everything, replace one mail — is expressible
     * in one call: `AuthEmailTemplates(layout = brand, accountActivation = MyActivation(brand))`.
     * Overriding a template and branding the other two used to be mutually exclusive, which meant an
     * app doing both silently shipped framework chrome on the two it did not name.
     */
    layout: EmailLayout = EmailLayout.default,
    val accountActivation: AccountActivationEmailTemplate = DefaultAccountActivationEmailTemplate(layout),
    val passwordChanged: PasswordChangedEmailTemplate = DefaultPasswordChangedEmailTemplate(layout),
    val passwordRecovery: PasswordRecoveryEmailTemplate = DefaultPasswordRecoveryEmailTemplate(layout),
) {
    companion object {
        /** The three framework defaults sharing one [layout]. Same as `AuthEmailTemplates(layout)`. */
        fun default(layout: EmailLayout = EmailLayout.default) = AuthEmailTemplates(layout = layout)
    }

    init {
        // At WIRING time rather than per email, so a template that cannot render its own guaranteed
        // locale surfaces when the realm is constructed.
        //
        // NOTE: realms are registered `dynamic` in kontainer, i.e. rebuilt per request, so this is
        // "first request that touches the realm", not literally application boot. Making it a true
        // boot check means hoisting AuthEmailTemplates to a singleton — worth doing, but it is a
        // kontainer-scoping change, not a template one.
        accountActivation.validate()
        passwordChanged.validate()
        passwordRecovery.validate()
    }
}

/**
 * The closing line shared by all three mails.
 *
 * Extracted because it was six identical copies: a footer change should be one edit, and six copies
 * are how one of them silently keeps saying something else.
 */
internal fun BODY.authEmailSignature(locale: Locale, senderName: String) {
    p {
        when (locale.language) {
            "de" -> +"Viele Grüße"
            else -> +"Yours sincerely,"
        }
        br()
        +senderName
    }
}

/**
 * The locale preference for a mail to [user], most-specific first: the user's own setting, then
 * [realmDefault]. Templates append the locale they guarantee, so this list needs no last resort.
 *
 * A LIST rather than "the user's setting, or else the realm default". [realmDefault] sits BETWEEN the
 * user's language and the framework's `en`, and collapsing that into an either/or is how a
 * German-only product mails framework English to a user whose stored language it has no rendering
 * for.
 *
 * `Locale.parse` is what makes a stored `"DE"` or `"de_CH"` resolve like the `de-CH` it means instead
 * of missing every rendering and silently falling back to English.
 *
 * Public because an app supplying its own `AuthRealm.Messaging` needs the same rule; deriving it a
 * second time by hand is how the two drift.
 */
fun authEmailLocales(user: AuthUser, realmDefault: Locale): List<Locale> {
    val fromUser = user.language.messaging
        ?.takeIf { it.isNotBlank() }
        ?.let { Locale.parse(it) }

    return listOfNotNull(fromUser, realmDefault)
}

/**
 * Fails unless [email] is addressed to exactly [recipient], with no cc and no bcc.
 *
 * A template builds its own envelope, so this is the framework re-asserting the one part of it that
 * must not be wrong. These mails carry live single-use tokens: an app template that adds a cc "so
 * support can see onboarding mails", or that slips and addresses `senderEmail`, hands out working
 * credentials — and `EmailStoring`'s anonymization protects the stored copy, not the delivery.
 *
 * A separate function rather than an inline check so the rule is testable on its own; the call site
 * runs it inside a try, so a violation degrades to a failed `EmailResult` rather than a 500.
 */
internal fun requireAuthEmailEnvelope(email: Email, recipient: EmailAddress) {
    require(email.destination == EmailDestination.to(recipient.value)) {
        "An auth email must be addressed to exactly the user it concerns, with no cc/bcc; " +
                "got ${email.destination}"
    }
}

/**
 * Assembles one auth email around [content].
 *
 * Everything downstream of the message itself — delivery, the storing policy, anonymization — belongs
 * to the caller, so this deliberately does NOT attach an [io.peekandpoke.funktor.messaging.storage.EmailStoring].
 * A template that could choose its own storing policy could choose to persist its own live token.
 */
internal fun authEmail(
    layout: EmailLayout,
    senderEmail: String,
    recipient: EmailAddress,
    subject: String,
    locale: Locale,
    content: BODY.() -> Unit,
): Email = Email(
    source = senderEmail,
    destination = EmailDestination.to(recipient.value),
    subject = subject,
    body = layout.render(locale, content),
)
