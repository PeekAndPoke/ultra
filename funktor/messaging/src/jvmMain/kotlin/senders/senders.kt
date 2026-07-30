package io.peekandpoke.funktor.messaging.senders

import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.messaging.EmailHooks
import io.peekandpoke.funktor.messaging.EmailSender
import io.peekandpoke.funktor.messaging.MailingDevConfig
import io.peekandpoke.funktor.messaging.MailingOverrides
import io.peekandpoke.ultra.log.Log

/**
 * Applies the DEV behaviour: recipient redirection, the dev banner, ignored domains, and
 * `disableEmails`.
 *
 * It knows nothing about test mode. `funktorMessaging` only reaches this when the app is NOT running
 * in test mode — there, it substitutes a [CapturingEmailSender] before composition and never consults
 * the app's provider at all. Exactly one place decides what test mode means, and this is not it.
 */
fun EmailSender.applyDevConfig(config: AppConfig, devConfig: MailingDevConfig?): EmailSender {
    devConfig ?: return this

    val sender = if (devConfig.disableEmails) {
        NullEmailSender()
    } else {
        this
    }

    return sender.ignoreExampleDomains(devConfig.ignoreDomains)
        .withOverrides { developmentMode(config, devConfig) }
}

fun EmailSender.withOverrides(
    override: MailingOverrides.Builder.() -> Unit,
): EmailSender {

    val built = MailingOverrides.build(override)

    return OverridingEmailSender(
        override = built,
        wrapped = this
    )
}

fun EmailSender.ignoreExampleDomains(
    domains: List<String> = ExampleDomainsIgnoringEmailSender.defaultDomains,
): EmailSender {
    return ExampleDomainsIgnoringEmailSender(
        domains = domains,
        wrapped = this
    )
}

fun EmailSender.withHooks(log: Log, hooks: EmailHooks.Builder.() -> Unit): EmailSender {
    val built = EmailHooks.Builder(log).apply(hooks).build()

    return HooksEmailSender(
        hooks = built,
        wrapped = this
    )
}
