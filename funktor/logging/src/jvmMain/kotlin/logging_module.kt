package de.peekandpoke.ktorfx.logging

import de.peekandpoke.karango.vault.KarangoDriver
import de.peekandpoke.ktorfx.core.kontainer
import de.peekandpoke.ktorfx.logging.api.LoggingApiFeature
import de.peekandpoke.ktorfx.logging.karango.KarangoLogAppender
import de.peekandpoke.ktorfx.logging.karango.KarangoLogRepository
import de.peekandpoke.ktorfx.logging.karango.KarangoLogsStorage
import de.peekandpoke.ultra.kontainer.KontainerAware
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module
import de.peekandpoke.ultra.logging.LogLevel
import de.peekandpoke.ultra.logging.Slf4jAppender
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

fun KontainerBuilder.ktorFxLogging(
    builder: KtorFXLoggingBuilder.() -> Unit = {},
) = module(KtorFX_Logging, builder)

inline val KontainerAware.logging: LoggingFacade get() = kontainer.get()
inline val ApplicationCall.logging: LoggingFacade get() = kontainer.get(LoggingFacade::class)
inline val RoutingContext.logging: LoggingFacade get() = call.logging

/**
 * Logging kontainer module
 */
val KtorFX_Logging = module { builder: KtorFXLoggingBuilder.() -> Unit ->
    singleton(LoggingFacade::class)

    // Services and defaults
    singleton(LogsStorage::class, LogsStorage.Null::class)

    // Api
    singleton(LoggingApiFeature::class)

    // Apply external configuration
    KtorFXLoggingBuilder(this).apply(builder)

    // Log appender for Slf4J TODO: make it configurable through ktorFxLogging
    instance(Slf4jAppender(LoggerFactory.getLogger(Application::class.java)))
}

class KtorFXLoggingBuilder internal constructor(private val kontainer: KontainerBuilder) {

    companion object {
        const val defaultLogCollectionName: String = "system_logs"
    }

    fun useKarango(repoName: String = defaultLogCollectionName) {
        with(kontainer) {
            // Add a log appender for the database
            singleton(KarangoLogAppender::class) { repo: Lazy<KarangoLogRepository> ->
                KarangoLogAppender(
                    repo = repo,
                    minLevel = LogLevel.INFO,
                )
            }
            // Add the repository for the log entries
            singleton(KarangoLogRepository::class) { driver: KarangoDriver ->
                KarangoLogRepository(driver = driver, repoName = repoName)
            }
            // Add logs reader
            singleton(LogsStorage::class, KarangoLogsStorage::class)
        }
    }
}
