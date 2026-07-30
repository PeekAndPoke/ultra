package io.peekandpoke.funktor.messaging.senders

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.config.ktor.KtorConfig
import io.peekandpoke.funktor.messaging.Email
import io.peekandpoke.funktor.messaging.EmailSender
import io.peekandpoke.funktor.messaging.MailingDevConfig
import io.peekandpoke.funktor.messaging.api.EmailBody
import io.peekandpoke.funktor.messaging.api.EmailDestination
import io.peekandpoke.funktor.messaging.api.EmailResult

/**
 * `applyDevConfig` owns the DEV behaviour and nothing else.
 *
 * Test mode is decided in exactly ONE place — the `Mailing` composition in `Funktor_Messaging`, which
 * substitutes a capturing sender and never reaches this function at all. `MailingTestModeGuardSpec`
 * covers that half. Two places deciding what "test" means is what this split exists to prevent.
 */
class ApplyDevConfigSpec : StringSpec({

    /** Stands in for a real provider: it delivers, and records what it was asked to deliver. */
    class DeliveringSpy : EmailSender {
        val delivered = mutableListOf<Email>()

        override suspend fun send(email: Email): EmailResult {
            delivered.add(email)
            return EmailResult(success = true, messageId = "spy", attributes = emptyMap())
        }
    }

    fun config(environment: String) = AppConfig.of(
        ktor = KtorConfig(deployment = KtorConfig.Deployment(environment = environment))
    )

    val email = Email(
        source = "sender@corp.test",
        destination = EmailDestination.to("user@corp.test"),
        subject = "Subject",
        body = EmailBody.Text("body"),
    )

    "no dev config leaves the sender completely untouched" {
        val provider = DeliveringSpy()

        val subject = provider.applyDevConfig(config = config("prod"), devConfig = null)

        // The SAME instance, not a no-op wrapper: `OverridingEmailSender` attaches attributes to
        // every result it passes through, so wrapping unconditionally would change what production
        // records for every mail it ever sends.
        subject shouldBeSameInstanceAs provider

        subject.send(email)

        provider.delivered.size shouldBe 1
    }

    "disableEmails must not deliver" {
        val provider = DeliveringSpy()

        provider.applyDevConfig(
            config = config("dev"),
            devConfig = MailingDevConfig(disableEmails = true),
        ).send(email)

        provider.delivered shouldBe emptyList()
    }

    "a dev destination redirects the recipient" {
        val provider = DeliveringSpy()

        provider.applyDevConfig(
            config = config("dev"),
            devConfig = MailingDevConfig(destination = "dev-inbox@corp.test"),
        ).send(email)

        val sent = provider.delivered.singleOrNull().shouldNotBeNull()

        sent.destination.toAddresses shouldBe listOf("dev-inbox@corp.test")
    }

    "the dev banner prefixes the subject with app id and environment" {
        val provider = DeliveringSpy()

        provider.applyDevConfig(
            config = config("dev"),
            devConfig = MailingDevConfig(destination = "dev-inbox@corp.test"),
        ).send(email)

        val sent = provider.delivered.singleOrNull().shouldNotBeNull()

        sent.subject shouldBe "[Application | dev]Subject"
    }

    "ignored domains are dropped — and the REDIRECT is applied FIRST" {
        // Order is load-bearing and easy to reverse by accident: `applyDevConfig` puts the overrides
        // OUTSIDE the domain filter, so the filter judges the address the mail is actually going to.
        // With a dev destination configured, mail addressed to an ignored domain is redirected to the
        // dev inbox and therefore still sent. Reverse the two and it would be silently dropped —
        // which reads as "dev mail stopped working" and points nowhere near the cause.
        val toIgnoredDomain = email.copy(destination = EmailDestination.to("someone@example.com"))

        val withRedirect = DeliveringSpy()
        withRedirect.applyDevConfig(
            config = config("dev"),
            devConfig = MailingDevConfig(
                destination = "dev-inbox@corp.test",
                ignoreDomains = listOf("example.com"),
            ),
        ).send(toIgnoredDomain)

        withRedirect.delivered.singleOrNull().shouldNotBeNull()
            .destination.toAddresses shouldBe listOf("dev-inbox@corp.test")

        val withoutRedirect = DeliveringSpy()
        withoutRedirect.applyDevConfig(
            config = config("dev"),
            devConfig = MailingDevConfig(ignoreDomains = listOf("example.com")),
        ).send(toIgnoredDomain)

        withoutRedirect.delivered shouldBe emptyList()
    }
})
