package io.peekandpoke.funktor.messaging

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.config.ktor.KtorConfig
import io.peekandpoke.funktor.messaging.api.EmailAttachment
import io.peekandpoke.funktor.messaging.api.EmailBody
import io.peekandpoke.funktor.messaging.api.EmailDestination
import io.peekandpoke.funktor.messaging.api.EmailResult
import io.peekandpoke.funktor.messaging.api.SentMessageModel
import io.peekandpoke.funktor.messaging.senders.CapturedEmails
import io.peekandpoke.funktor.messaging.storage.EmailStoring
import io.peekandpoke.funktor.messaging.storage.EmailStoring.Companion.store
import io.peekandpoke.funktor.messaging.storage.SentMessage
import io.peekandpoke.funktor.messaging.storage.SentMessagesStorage
import io.peekandpoke.ultra.kontainer.KontainerBlueprint
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.log.ultraLogging
import io.peekandpoke.ultra.model.PagedSearchFilter
import io.peekandpoke.ultra.vault.Cursor
import io.peekandpoke.ultra.vault.Stored

/**
 * What the app no longer has to do for itself.
 *
 * Before this, an app wired `provider.applyDevConfig(...).withHooks(log) { onAfterSend(storing) }` by
 * hand, and omitting any link was silent. These pin that the module now applies each one, and that
 * the order the app writes its builder calls in cannot change the result.
 */
class FunktorMessagingBuilderSpec : StringSpec({

    class DeliveringSpy : EmailSender {
        val delivered = mutableListOf<Email>()

        override suspend fun send(email: Email): EmailResult {
            delivered.add(email)
            return EmailResult(success = true, messageId = "spy", attributes = emptyMap())
        }
    }

    /** Records what the storing hook was asked to persist. */
    class RecordingStorage : SentMessagesStorage {
        val stored = mutableListOf<SentMessageModel.Content.EmailContent>()

        override suspend fun clear() = Unit

        override suspend fun findByRefs(
            refs: Set<String>,
            filter: PagedSearchFilter,
        ): Cursor<Stored<SentMessage>> = Cursor.empty()

        override suspend fun storeSentEmail(
            result: EmailResult,
            refs: Set<String>,
            tags: Set<String>,
            content: SentMessageModel.Content.EmailContent,
            attachments: List<EmailAttachment>,
        ) {
            stored.add(content)
        }
    }

    fun config(environment: String): AppConfig = AppConfig.of(
        ktor = KtorConfig(deployment = KtorConfig.Deployment(environment = environment))
    )

    fun mail(subject: String = "Subject") = Email(
        source = "sender@corp.test",
        destination = EmailDestination.to("user@corp.test"),
        subject = subject,
        body = EmailBody.Text("body"),
    )

    fun blueprint(
        environment: String,
        storage: SentMessagesStorage? = null,
        messaging: FunktorMessagingBuilder.() -> Unit,
    ): KontainerBlueprint = kontainer {
        instance(config(environment))
        ultraLogging()
        funktorMessaging(config(environment), messaging)
        if (storage != null) {
            singleton(SentMessagesStorage::class) { storage }
        }
    }

    "dev config is applied WITHOUT the app ever calling applyDevConfig" {
        val provider = DeliveringSpy()

        val subject = blueprint("dev") {
            useSender(devConfig = MailingDevConfig(destination = "dev-inbox@corp.test")) { provider }
        }.create()

        subject.get(Mailing::class).send(mail())

        val sent = provider.delivered.singleOrNull().shouldNotBeNull()

        sent.destination.toAddresses shouldBe listOf("dev-inbox@corp.test")
        sent.subject shouldStartWith "[Application | dev]"
    }

    "the storing hook is wired WITHOUT the app asking for it" {
        val storage = RecordingStorage()
        val provider = DeliveringSpy()

        val subject = blueprint("dev", storage = storage) { useSender { provider } }.create()

        subject.get(Mailing::class).send(
            mail().store(EmailStoring.withContent(tags = setOf("greeting")))
        )

        storage.stored.single().subject shouldBe "Subject"
    }

    "the storing hook persists the ORIGINAL mail, not the dev-decorated one" {
        // Hooks are composed OUTSIDE the overrides on purpose. If that order is ever reversed, the
        // database starts recording dev-prefixed subjects and redirected recipients as though they
        // were what the user received — and nothing else in the suite would notice.
        val storage = RecordingStorage()

        val subject = blueprint("dev", storage = storage) {
            useSender(devConfig = MailingDevConfig(destination = "dev-inbox@corp.test")) { DeliveringSpy() }
        }.create()

        subject.get(Mailing::class).send(
            mail().store(EmailStoring.withContent())
        )

        val stored = storage.stored.single()

        stored.subject shouldBe "Subject"
        stored.destination.toAddresses shouldBe listOf("user@corp.test")
    }

    "calling useSender twice replaces the previous sender" {
        val first = DeliveringSpy()
        val second = DeliveringSpy()

        val subject = blueprint("dev") {
            useSender { first }
            useSender { second }
        }.create()

        subject.get(Mailing::class).send(mail())

        first.delivered shouldBe emptyList()
        second.delivered.size shouldBe 1
    }

    "a second useSender without a devConfig must NOT silently drop the first one" {
        // The dangerous shape: a merge, a conditional block or a second module adds a plain
        // `useSender { … }`. If that cleared devConfig, dev-mode redirection would vanish and every
        // mail from a dev box would go to the real customer address, with no banner and no error.
        val second = DeliveringSpy()

        val subject = blueprint("dev") {
            useSender(devConfig = MailingDevConfig(destination = "dev-inbox@corp.test")) { DeliveringSpy() }
            useSender { second }
        }.create()

        subject.get(Mailing::class).send(mail())

        second.delivered.single().destination.toAddresses shouldBe listOf("dev-inbox@corp.test")
    }

    "a mail with NO storing policy is not persisted" {
        // `StoringEmailHook` no-ops when `email.storing()` is null. Now that the module always wires
        // the hook, this is the only thing keeping deliberately-unretained mail out of the database
        // — and a change of that default would put full bodies into it with nothing going red.
        val storage = RecordingStorage()

        val subject = blueprint("dev", storage = storage) { useSender { DeliveringSpy() } }.create()

        subject.get(Mailing::class).send(mail())

        storage.stored shouldBe emptyList()
    }

    "withoutContent persists the metadata but NOT the body" {
        val storage = RecordingStorage()

        val subject = blueprint("dev", storage = storage) { useSender { DeliveringSpy() } }.create()

        subject.get(Mailing::class).send(
            mail().store(EmailStoring.withoutContent(tags = setOf("audit")))
        )

        val stored = storage.stored.single()

        stored.subject shouldBe "Subject"
        (stored.body as EmailBody.Text).content shouldBe "n/a"
    }

    "a throwing hook must not break the send, nor stop the other hooks" {
        // `EmailHooks` runs hooks in a supervisorScope with a per-hook catch. Drop either and an
        // unreachable sent-messages database stops password-reset mail for every app in the
        // framework — the send itself would start throwing.
        val storage = RecordingStorage()

        val subject = blueprint("dev", storage = storage) {
            useSender { DeliveringSpy() }
            onAfterSend { _, _ -> error("this hook always fails") }
        }.create()

        val result = subject.get(Mailing::class).send(
            mail().store(EmailStoring.withContent())
        )

        result.success shouldBe true
        storage.stored.size shouldBe 1
    }

    "an app hook runs alongside the framework's, and sees the ORIGINAL mail" {
        val seen = mutableListOf<Email>()

        val subject = blueprint("dev") {
            useSender(devConfig = MailingDevConfig(destination = "dev-inbox@corp.test")) { DeliveringSpy() }
            onAfterSend { email, _ -> seen.add(email) }
        }.create()

        subject.get(Mailing::class).send(mail())

        // Same position in the chain as the storing hook — outside the overrides — so an app hook and
        // a framework hook cannot disagree about what was sent.
        seen.single().destination.toAddresses shouldBe listOf("user@corp.test")
    }

    "the provider is constructed at most ONCE, however many kontainers are created" {
        // The composed Mailing is rebuilt per kontainer (it injects dynamics), so a provider built
        // inside that factory would open a new SDK client on EVERY request. The EmailSender
        // registration deliberately has no dependencies, which keeps it a global singleton.
        var constructed = 0

        val blueprint = blueprint("dev") {
            useSender {
                constructed++
                DeliveringSpy()
            }
        }

        repeat(3) { blueprint.create().get(Mailing::class).send(mail()) }

        constructed shouldBe 1
    }

    "captured mail is visible across kontainers created from one blueprint" {
        // The regression test for the scoping trap. `Mailing` is SemiDynamic — one instance per
        // kontainer, i.e. per request — so the capturing sender is rebuilt each time. Only because
        // CapturedEmails has NO constructor dependencies is it a global singleton that every one of
        // those senders writes into, and therefore the one a test can read. Give CapturedEmails a
        // dependency and this test goes red while every single-kontainer test stays green.
        val blueprint = blueprint("test") { }

        blueprint.create().get(Mailing::class).send(mail(subject = "from-kontainer-1"))
        blueprint.create().get(Mailing::class).send(mail(subject = "from-kontainer-2"))

        blueprint.create().get(CapturedEmails::class).captured.map { it.subject } shouldBe
                listOf("from-kontainer-1", "from-kontainer-2")
    }
})
