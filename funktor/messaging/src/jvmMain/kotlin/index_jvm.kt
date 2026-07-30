package io.peekandpoke.funktor.messaging

import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.RoutingContext
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.funktor.messaging.fixtures.SentMessagesFixtures
import io.peekandpoke.funktor.messaging.api.EmailResult
import io.peekandpoke.funktor.messaging.senders.CapturedEmails
import io.peekandpoke.funktor.messaging.senders.ConfiguredEmailSender
import io.peekandpoke.funktor.messaging.senders.CapturingEmailSender
import io.peekandpoke.funktor.messaging.senders.NullEmailSender
import io.peekandpoke.funktor.messaging.senders.applyDevConfig
import io.peekandpoke.funktor.messaging.senders.withHooks
import io.peekandpoke.funktor.messaging.storage.SentMessagesStorage
import io.peekandpoke.funktor.messaging.storage.StoringEmailHook
import io.peekandpoke.funktor.messaging.storage.karango.KarangoSentMessagesRepo
import io.peekandpoke.funktor.messaging.storage.monko.MonkoSentMessagesRepo
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.ultra.kontainer.KontainerAware
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.module
import io.peekandpoke.ultra.log.Log
import io.peekandpoke.ultra.log.NullLog
import io.peekandpoke.ultra.vault.hooks.TimestampedHook

/**
 * [config] is taken explicitly, the way [io.peekandpoke.funktor.core.funktorCore] and
 * `funktorRest` do, rather than injected from the kontainer: the module needs it to decide whether
 * mail may leave the process, and an explicit parameter makes every call site supply it at compile
 * time instead of discovering at boot that no `AppConfig` happened to be registered.
 */
fun KontainerBuilder.funktorMessaging(
    config: AppConfig,
    builder: FunktorMessagingBuilder.() -> Unit = {},
) = module(Funktor_Messaging, config, builder)

inline val KontainerAware.funktorMessaging: MessagingServices get() = kontainer.get()
inline val ApplicationCall.funktorMessaging: MessagingServices get() = kontainer.funktorMessaging
inline val RoutingContext.funktorMessaging: MessagingServices get() = call.funktorMessaging

val Funktor_Messaging = module { config: AppConfig, builder: FunktorMessagingBuilder.() -> Unit ->
    // Facade
    singleton(MessagingServices::class)

    // The app's provider, if it configured one — see ConfiguredEmailSender for why it is NOT
    // registered under `EmailSender`. `FunktorMessagingBuilder.useSender` replaces this.
    singleton(ConfiguredEmailSender::class) { ConfiguredEmailSender { NullEmailSender() } }

    // Mailing Hooks
    dynamic(StoringEmailHook::class)
    singleton(SentMessagesStorage::class, SentMessagesStorage.Null::class)

    // Test-mode capture. No constructor dependencies, therefore a GLOBAL singleton — see the class
    // KDoc. Always registered: in production nothing writes to it.
    singleton(CapturedEmails::class)

    // Fixtures
    singleton(SentMessagesFixtures::class)

    /////////////////////////////////////////////////////////////////////////////////
    // Apply external configuration
    val messaging = FunktorMessagingBuilder(kontainer = this).apply(builder)

    // Mailing — THE composition site, and deliberately the only one.
    //
    // Everything an app used to chain by hand now happens here: dev config, the storing hook, the
    // debug log. An app supplies a provider and nothing else, so there is no line to forget.
    //
    // Registered after `apply(builder)` so no builder call can displace it, and so builder methods
    // may be written in any order — every field read below is read when a kontainer is created, long
    // after the builder has run.
    singleton(Mailing::class) { configured: ConfiguredEmailSender, log: Log?, storing: StoringEmailHook,
                                captured: CapturedEmails ->

        val effectiveLog = log ?: NullLog

        val base = when {
            // In test mode the app's provider is not consulted AT ALL — `configured.sender` is never
            // touched, so `AwsSesSender.of(...)` never even runs and no credentials are needed.
            config.ktor.isTest -> CapturingEmailSender(captured)

            else -> configured.sender.applyDevConfig(config, messaging.devConfig)
        }

        // Hooks go OUTSIDE the overrides, which is load-bearing: `StoringEmailHook` must persist the
        // ORIGINAL mail, not the dev-prefixed and redirected one. Do not reorder this.
        SimpleMailing(
            base.withHooks(effectiveLog) {
                onAfterSend(storing)
                messaging.extraHooks.forEach { onAfterSend(it) }
                // Identity only, NEVER the mail itself. Interpolating `$email` would put the body —
                // for auth mail, a live reset token — into every appender, and `LogCollector` has no
                // level filter, so it would be persisted into insights records and rendered by a GUI
                // route with no auth floor.
                onAfterSend { email, result ->
                    effectiveLog.debug(
                        "Email sent to ${email.destination.toAddresses}, " +
                                "subject='${email.subject}', messageId=${result.messageId}"
                    )
                }
            }
        )
    }
}

class FunktorMessagingBuilder internal constructor(
    private val kontainer: KontainerBuilder,
) {
    /** Read by the [Mailing] factory, so builder methods may be called in any order. */
    internal var devConfig: MailingDevConfig? = null

    internal val extraHooks = mutableListOf<EmailHooks.OnAfterSend>()

    /**
     * Adds an app hook, run alongside the ones this module always installs.
     *
     * Use this rather than wrapping the sender inside [useSender]: a hook added here sits in the SAME
     * place in the chain as the storing hook — outside the dev overrides — so it observes the mail as
     * the app composed it, and it still runs in test mode where the app's provider is never
     * constructed. A hook buried inside the provider lambda would see the dev-redirected mail and
     * would never run under test.
     */
    fun onAfterSend(hook: EmailHooks.OnAfterSend) {
        extraHooks.add(hook)
    }

    /** @see onAfterSend */
    fun onAfterSend(block: suspend (email: Email, result: EmailResult) -> Unit) {
        onAfterSend(
            object : EmailHooks.OnAfterSend {
                override suspend fun invoke(email: Email, result: EmailResult) = block(email, result)
            }
        )
    }

    /**
     * The one thing an app has to supply: how to reach its mail provider.
     *
     * Everything else — dev config, test-mode capture, the storing hook, the debug log — is applied
     * by this module, so it cannot be omitted.
     *
     * [provider] is a lambda, not a value, for two reasons: it is invoked at most once per blueprint
     * (the registration has no dependencies, so it stays a global singleton), and it is never invoked
     * at all in test mode. `AwsSesSender.of(...)` opens an SDK client, so neither is incidental.
     *
     * [devConfig] is app-specific data the framework cannot reach on its own — it hangs off the app's
     * own config class, not off [AppConfig].
     *
     * Calling this more than once replaces the previously configured sender.
     *
     * NOTE the provider cannot be a kontainer service: registering it under its own type would make
     * it a second candidate for every `EmailSender` injection point and the blueprint would fail
     * validation as ambiguous. Construct it in the lambda.
     */
    fun useSender(devConfig: MailingDevConfig? = null, provider: () -> EmailSender) {
        // Only ever SET, never cleared. A second `useSender` written without a devConfig — a merge, a
        // conditional block, a second module — would otherwise silently drop dev-mode redirection and
        // start mailing real addresses from a dev box.
        devConfig?.let { this.devConfig = it }

        with(kontainer) {
            singleton(ConfiguredEmailSender::class) { ConfiguredEmailSender(provider) }
        }
    }

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
