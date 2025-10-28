package de.peekandpoke.funktor.messaging

import de.peekandpoke.funktor.core.kontainer
import de.peekandpoke.funktor.messaging.fixtures.SentMessagesFixtures
import de.peekandpoke.funktor.messaging.senders.NullEmailSender
import de.peekandpoke.funktor.messaging.storage.SentMessagesStorage
import de.peekandpoke.funktor.messaging.storage.StoringEmailHook
import de.peekandpoke.funktor.messaging.storage.karango.KarangoSentMessagesRepo
import de.peekandpoke.karango.vault.KarangoDriver
import de.peekandpoke.ultra.kontainer.KontainerAware
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module
import de.peekandpoke.ultra.vault.hooks.TimestampedHook
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun KontainerBuilder.funktorMessaging(
    builder: FunktorMessagingBuilder.() -> Unit = {},
) = module(Funktor_Messaging, builder)

inline val KontainerAware.funktorMessaging: MessagingServices get() = kontainer.get()
inline val ApplicationCall.funktorMessaging: MessagingServices get() = kontainer.funktorMessaging
inline val RoutingContext.funktorMessaging: MessagingServices get() = call.funktorMessaging

val Funktor_Messaging = module { builder: FunktorMessagingBuilder.() -> Unit ->
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
    FunktorMessagingBuilder(this).apply(builder)
}

class FunktorMessagingBuilder internal constructor(private val kontainer: KontainerBuilder) {

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
