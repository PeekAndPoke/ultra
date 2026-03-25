package io.peekandpoke.funktor.logging.karango

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxy
import ch.qos.logback.core.UnsynchronizedAppenderBase
import io.ktor.server.application.*
import io.peekandpoke.funktor.logging.FunktorLoggingBuilder
import io.peekandpoke.karango.config.ArangoDbConfig
import io.peekandpoke.karango.getKarangoDefaultSlumberConfig
import io.peekandpoke.karango.slumber.KarangoCodec
import io.peekandpoke.karango.toArangoDbWithoutCache
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.ultra.common.network.NetworkUtils
import io.peekandpoke.ultra.log.LogLevel
import io.peekandpoke.ultra.vault.Database
import io.peekandpoke.ultra.vault.NullEntityCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun Application.addKarangoAppender(
    config: ArangoDbConfig,
    minLevel: Level = Level.INFO,
    repoName: String = FunktorLoggingBuilder.defaultLogCollectionName,
) {
    when (val l = log) {
        is ch.qos.logback.classic.Logger -> {

            if (l.getAppender(LogbackKarangoLogAppender.Name) == null) {

                l.addAppender(
                    LogbackKarangoLogAppender(
                        application = this,
                        config = config,
                        repoName = repoName,
                        minLevel = minLevel
                    ).apply { start() }
                )

                l.info("Added LogbackKarangoLogAppender to Logger '${l.name}' (${this::class.qualifiedName})")
            }
        }

        else -> {
            error("Could not add LogbackKarangoLogAppender to Logger '${l.name}' of type ${this::class.qualifiedName}")
        }
    }
}

class LogbackKarangoLogAppender(
    private val application: Application,
    config: ArangoDbConfig,
    repoName: String,
    private val minLevel: Level,
    private val serverName: String = NetworkUtils.getHostNameOrDefault(),
) : UnsynchronizedAppenderBase<ILoggingEvent>() {

    private var driver: KarangoDriver
    private var repository: KarangoLogRepository

    companion object {
        const val Name = "KarangoAppender"
    }

    init {
        // Create a new DB driver
        runBlocking {
            driver = KarangoDriver(
                lazyArangoDb = lazy {
                    config.toArangoDbWithoutCache()
                },
                lazyCodec = lazy {
                    KarangoCodec(
                        config = getKarangoDefaultSlumberConfig(),
                        database = Database.withNoRepos,
                        entityCache = NullEntityCache
                    )
                },
            )

            repository = KarangoLogRepository(driver = driver, repoName = repoName)
            repository.ensure()
        }
    }

    override fun getName(): String = Name

    override fun append(eventObject: ILoggingEvent) {

        if (eventObject.level.isGreaterOrEqual(minLevel)) {

            val entry = KarangoLogEntry(
                createdAt = eventObject.timeStamp,
                level = eventObject.level.map(),
                loggerName = eventObject.loggerName,
                message = eventObject.message,
                threadName = eventObject.threadName,
                stackTrace = (eventObject.throwableProxy as? ThrowableProxy)?.throwable?.stackTraceToString(),
                server = serverName,
            )

            application.launch(Dispatchers.IO) {
                repository.tryInsert(entry)
            }
        }
    }

    private fun Level.map(): LogLevel {
        return when (this) {
            Level.OFF -> LogLevel.OFF
            Level.ERROR -> LogLevel.ERROR
            Level.WARN -> LogLevel.WARNING
            Level.INFO -> LogLevel.INFO
            Level.DEBUG -> LogLevel.DEBUG
            Level.TRACE -> LogLevel.TRACE
            else -> LogLevel.ALL
        }
    }
}
