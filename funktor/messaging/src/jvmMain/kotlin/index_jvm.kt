package io.peekandpoke.funktor.messaging

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.funktor.messaging.fixtures.SentMessagesFixtures
import io.peekandpoke.funktor.messaging.senders.NullEmailSender
import io.peekandpoke.funktor.messaging.storage.SentMessagesStorage
import io.peekandpoke.funktor.messaging.storage.StoringEmailHook
import io.peekandpoke.funktor.messaging.storage.karango.KarangoSentMessagesRepo
import io.peekandpoke.funktor.messaging.storage.monko.MonkoSentMessagesRepo
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.ultra.kontainer.KontainerAware
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.module
import io.peekandpoke.ultra.vault.hooks.TimestampedHook

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

    fun useMonko(
        sentMessageRepoName: String = "system_sent_messages",
    ) {
        with(kontainer) {
            singleton(MonkoSentMessagesRepo::class) { driver: MonkoDriver, timestamped: TimestampedHook ->
                MonkoSentMessagesRepo(
                    name = sentMessageRepoName,
                    driver = driver,
                    timestamped = timestamped,
                )
            }

            singleton(SentMessagesStorage::class) { repo: MonkoSentMessagesRepo ->
                SentMessagesStorage.Vault(repo = repo)
            }
        }
    }
}
