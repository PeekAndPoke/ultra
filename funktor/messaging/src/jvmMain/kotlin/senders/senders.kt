package de.peekandpoke.funktor.messaging.senders

import de.peekandpoke.funktor.messaging.EmailHooks
import de.peekandpoke.funktor.messaging.EmailSender
import de.peekandpoke.funktor.messaging.MailingOverrides
import de.peekandpoke.ultra.log.Log

fun EmailSender.withOverrides(
    override: MailingOverrides.Builder.() -> Unit,
): EmailSender {

    val built = MailingOverrides.Builder().apply(override).build()

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

