package io.peekandpoke.ultra.log

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.slf4j.Logger
import org.slf4j.helpers.NOPLogger
import java.time.ZonedDateTime
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The appender must route by origin and hand the error to SLF4J's throwable channel, not render it
 * into the message.
 */
class Slf4jAppenderSpec : StringSpec({

    data class Call(val loggerName: String, val level: LogLevel, val message: String, val error: Throwable?)

    /** Records what would have been sent to SLF4J, and under which logger name. */
    class Recording : Slf4jAppender() {
        val calls = CopyOnWriteArrayList<Call>()
        private var current: String = ""

        override fun loggerFor(loggerName: String): Logger {
            current = loggerName
            return object : Logger by NOPLogger.NOP_LOGGER {
                override fun error(msg: String?, t: Throwable?) = record(LogLevel.ERROR, msg, t)
                override fun warn(msg: String?, t: Throwable?) = record(LogLevel.WARNING, msg, t)
                override fun info(msg: String?, t: Throwable?) = record(LogLevel.INFO, msg, t)
                override fun debug(msg: String?, t: Throwable?) = record(LogLevel.DEBUG, msg, t)
                override fun trace(msg: String?, t: Throwable?) = record(LogLevel.TRACE, msg, t)
            }
        }

        private fun record(level: LogLevel, msg: String?, t: Throwable?) {
            calls.add(Call(current, level, msg ?: "", t))
        }
    }

    val ts = ZonedDateTime.parse("2025-01-15T08:30:00+01:00[Europe/Berlin]")

    "each event goes to a logger named after its origin" {
        val appender = Recording()

        appender.append(LogEvent(ts, LogLevel.INFO, "a", "app.svc.Alpha"))
        appender.append(LogEvent(ts, LogLevel.INFO, "b", "app.svc.Beta"))

        appender.calls.map { it.loggerName } shouldContainExactly listOf("app.svc.Alpha", "app.svc.Beta")
    }

    "the message is passed through unabbreviated, with no logger-name prefix" {
        val appender = Recording()

        appender.append(LogEvent(ts, LogLevel.INFO, "hello", "io.peekandpoke.ultra.log.service.MyService"))

        appender.calls.single().message shouldBe "hello"
    }

    "the Throwable goes to slf4j's throwable channel" {
        val appender = Recording()
        val boom = IllegalStateException("kaboom")

        appender.append(LogEvent(ts, LogLevel.ERROR, "failed", "app.Svc", boom))

        val call = appender.calls.single()
        call.error shouldBe boom
        call.message shouldBe "failed"
    }

    "every level maps to its slf4j counterpart" {
        val appender = Recording()

        listOf(LogLevel.ERROR, LogLevel.WARNING, LogLevel.INFO, LogLevel.DEBUG, LogLevel.TRACE)
            .forEach { appender.append(LogEvent(ts, it, "m", "app.Svc")) }

        appender.calls.map { it.level } shouldContainExactly
                listOf(LogLevel.ERROR, LogLevel.WARNING, LogLevel.INFO, LogLevel.DEBUG, LogLevel.TRACE)
    }

    "the threshold sentinels produce no slf4j call" {
        val appender = Recording()

        appender.append(LogEvent(ts, LogLevel.OFF, "m", "app.Svc"))
        appender.append(LogEvent(ts, LogLevel.ALL, "m", "app.Svc"))

        appender.calls shouldContainExactly emptyList()
    }
})
