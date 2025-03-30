package de.peekandpoke.ktorfx.messaging

import de.peekandpoke.karango.vault.KarangoDriver
import de.peekandpoke.ktorfx.core.kontainer
import de.peekandpoke.ktorfx.messaging.fixtures.SentMessagesFixtures
import de.peekandpoke.ktorfx.messaging.senders.NullEmailSender
import de.peekandpoke.ktorfx.messaging.storage.SentMessagesStorage
import de.peekandpoke.ktorfx.messaging.storage.StoringEmailHook
import de.peekandpoke.ktorfx.messaging.storage.karango.KarangoSentMessagesRepo
import de.peekandpoke.ultra.kontainer.KontainerAware
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module
import de.peekandpoke.ultra.vault.hooks.TimestampedHook
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun KontainerBuilder.ktorFxMessaging(
    builder: KtorFXCMessagingBuilder.() -> Unit = {},
) = module(KtorFx_Messaging, builder)

inline val KontainerAware.ktorFxMessaging: MessagingServices get() = kontainer.get()
inline val ApplicationCall.ktorFxMessaging: MessagingServices get() = kontainer.ktorFxMessaging
inline val RoutingContext.ktorFxMessaging: MessagingServices get() = call.ktorFxMessaging

val KtorFx_Messaging = module { builder: KtorFXCMessagingBuilder.() -> Unit ->
    // Facade
    singleton(MessagingServices::class)

    // Mailing
    singleton(Mailing::class, SimpleMailing::class)
    singleton(EmailSender::class, NullEmailSender::class)

    // Mailing Hooks
    dynamic(EmailHooks::class)
    dynamic(StoringEmailHook::class)
    singleton(SentMessagesStorage::class, SentMessagesStorage.Null::class)

    // Fixtures
    singleton(SentMessagesFixtures::class)

    /////////////////////////////////////////////////////////////////////////////////
    // Apply external configuration
    KtorFXCMessagingBuilder(this).apply(builder)
}

class KtorFXCMessagingBuilder internal constructor(private val kontainer: KontainerBuilder) {

    fun useKarango(
        sentMessageRepoName: String = "system_sent_messages",
    ) {
        with(kontainer) {
            singleton(KarangoSentMessagesRepo::class) { driver: KarangoDriver, timestamped: TimestampedHook ->
                KarangoSentMessagesRepo(
                    driver = driver,
                    timestamped = timestamped,
                    repoName = sentMessageRepoName,
                )
            }

            singleton(SentMessagesStorage::class) { repo: KarangoSentMessagesRepo ->
                SentMessagesStorage.Vault(repo = repo)
            }
        }
    }
}
