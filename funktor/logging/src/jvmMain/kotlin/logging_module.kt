package io.peekandpoke.funktor.logging

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.funktor.logging.api.LoggingApiFeature
import io.peekandpoke.funktor.logging.karango.KarangoLogAppender
import io.peekandpoke.funktor.logging.karango.KarangoLogRepository
import io.peekandpoke.funktor.logging.karango.KarangoLogsStorage
import io.peekandpoke.funktor.logging.monko.MonkoLogAppender
import io.peekandpoke.funktor.logging.monko.MonkoLogRepository
import io.peekandpoke.funktor.logging.monko.MonkoLogsStorage
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.ultra.kontainer.KontainerAware
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.module
import io.peekandpoke.ultra.log.LogLevel
import io.peekandpoke.ultra.log.Slf4jAppender
import org.slf4j.LoggerFactory

/** Registers the Funktor logging module in the kontainer. */
fun KontainerBuilder.funktorLogging(
    builder: FunktorLoggingBuilder.() -> Unit = {},
) = module(Funktor_Logging, builder)

/** Gets the [LoggingFacade] from the kontainer. */
inline val KontainerAware.logging: LoggingFacade get() = kontainer.get()

/** Gets the [LoggingFacade] from the kontainer. */
inline val ApplicationCall.logging: LoggingFacade get() = kontainer.get(LoggingFacade::class)

/** Gets the [LoggingFacade] from the kontainer. */
inline val RoutingContext.logging: LoggingFacade get() = call.logging

/**
 * Logging kontainer module
 */
val Funktor_Logging = module { builder: FunktorLoggingBuilder.() -> Unit ->
    singleton(LoggingFacade::class)

    // Services and defaults
    singleton(LogsStorage::class, LogsStorage.Null::class)

    // Api
    singleton(LoggingApiFeature::class)

    // Apply external configuration
    FunktorLoggingBuilder(this).apply(builder)

    // Log appender for Slf4J TODO: make it configurable through FunktorLogging
    instance(Slf4jAppender(LoggerFactory.getLogger(Application::class.java)))
}

/** DSL builder for configuring the logging module backend (Karango or Monko). */
class FunktorLoggingBuilder internal constructor(private val kontainer: KontainerBuilder) {

    companion object {
        const val defaultLogCollectionName: String = "system_logs"
    }

    /** Configures logging to persist entries in ArangoDB via Karango. */
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

    /** Configures logging to persist entries in MongoDB via Monko. */
    fun useMonko(repoName: String = defaultLogCollectionName) {
        with(kontainer) {
            // Add a log appender for the database
            singleton(MonkoLogAppender::class) { repo: Lazy<MonkoLogRepository> ->
                MonkoLogAppender(
                    repo = repo,
                    minLevel = LogLevel.INFO,
                )
            }
            // Add the repository for the log entries
            singleton(MonkoLogRepository::class) { driver: MonkoDriver ->
                MonkoLogRepository(driver = driver, repoName = repoName)
            }
            // Add logs reader
            singleton(LogsStorage::class, MonkoLogsStorage::class)
        }
    }
}
