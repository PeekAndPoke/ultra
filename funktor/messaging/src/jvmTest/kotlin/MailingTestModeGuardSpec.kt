package io.peekandpoke.funktor.messaging

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.config.ktor.KtorConfig
import io.peekandpoke.funktor.messaging.api.EmailBody
import io.peekandpoke.funktor.messaging.api.EmailDestination
import io.peekandpoke.funktor.messaging.api.EmailResult
import io.peekandpoke.funktor.messaging.senders.CapturedEmails
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.log.ultraLogging

/**
 * Test mode must never deliver — and that has to hold however the app wired its sender.
 *
 * These boot the real messaging module rather than testing a function in isolation, because the
 * failure being guarded against is a WIRING mistake. Everything sends through [Mailing]
 * (`realm.deps.messaging.mailing.send(...)`), which is where the module composes the chain, so that
 * is what these resolve.
 */
class MailingTestModeGuardSpec : StringSpec({

    class DeliveringSpy : EmailSender {
        val delivered = mutableListOf<Email>()

        override suspend fun send(email: Email): EmailResult {
            delivered.add(email)
            return EmailResult(success = true, messageId = "spy", attributes = emptyMap())
        }
    }

    fun config(environment: String): AppConfig = AppConfig.of(
        ktor = KtorConfig(deployment = KtorConfig.Deployment(environment = environment))
    )

    val email = Email(
        source = "sender@corp.test",
        destination = EmailDestination.to("user@corp.test"),
        subject = "Subject",
        body = EmailBody.Text("body"),
    )

    fun appKontainer(
        environment: String,
        messaging: FunktorMessagingBuilder.() -> Unit = {},
        extra: KontainerBuilder.() -> Unit = {},
    ): Kontainer = kontainer {
        instance(config(environment))
        ultraLogging()
        funktorMessaging(config(environment), messaging)
        extra()
    }.create()

    "an app that registers its sender DIRECTLY, bypassing useSender, must still not deliver in test mode" {
        // DO NOT rewrite this to call `useSender`. The direct `singleton(EmailSender::class)` below
        // IS the assertion: kontainer lets an app register whatever it likes, so the guarantee has to
        // hold for a sender the framework never composed. Rewriting it to the supported path would
        // leave this green while proving nothing.
        //
        // Since `EmailSender` is no longer a service the module reads (the provider lives behind
        // `ConfiguredEmailSender`), such a registration is now INERT rather than merely overridden —
        // which is the stronger property, and the one pinned here.
        val provider = DeliveringSpy()

        val subject = appKontainer(
            environment = "test",
            extra = { singleton(EmailSender::class) { provider } },
        )

        subject.get(Mailing::class).send(email)

        provider.delivered shouldBe emptyList()
        // ...and it is not merely dropped: the mail is still observable to the test.
        subject.get(CapturedEmails::class).captured.size shouldBe 1
    }

    "test mode captures instead of sending, with no wiring at all" {
        val subject = appKontainer(environment = "test")

        subject.get(Mailing::class).send(email)

        subject.get(CapturedEmails::class).captured.single().subject shouldBe "Subject"
    }

    "in test mode the app's provider is never even CONSTRUCTED" {
        // `AwsSesSender.of(...)` opens an SDK client. `ConfiguredEmailSender` builds the provider
        // behind a `by lazy` that the Mailing factory never touches in test mode, so a test pays
        // nothing for an expensive provider and needs no credentials for one.
        val subject = appKontainer(
            environment = "test",
            messaging = { useSender { error("the provider must not be constructed in test mode") } },
        )

        subject.get(Mailing::class).send(email)

        subject.get(CapturedEmails::class).captured.size shouldBe 1
    }

    "outside test mode the provider IS used, and nothing is captured" {
        val provider = DeliveringSpy()

        val subject = appKontainer(
            environment = "prod",
            messaging = { useSender { provider } },
        )

        subject.get(Mailing::class).send(email)

        provider.delivered.size shouldBe 1
        subject.get(CapturedEmails::class).captured shouldBe emptyList()
    }
})
