package io.peekandpoke.ultra.log

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.ZonedDateTime

class LogImplSpec : StringSpec() {

    /**
     * A test appender that records all log events for later inspection.
     */
    private class RecordingAppender : LogAppender {
        data class Entry(val ts: ZonedDateTime, val level: LogLevel, val message: String, val loggerName: String)

        val entries = mutableListOf<Entry>()

        override suspend fun append(ts: ZonedDateTime, level: LogLevel, message: String, loggerName: String) {
            entries.add(Entry(ts, level, message, loggerName))
        }
    }

    init {

        "getLogger returns a LogImpl" {
            val manager = UltraLogManager(emptyList())
            val log = manager.getLogger(LogImplSpec::class)

            log.shouldBeInstanceOf<LogImpl>()
        }

        "LogImpl.log dispatches to all appenders" {
            val appender1 = RecordingAppender()
            val appender2 = RecordingAppender()
            val manager = UltraLogManager(listOf(appender1, appender2))
            val log = manager.getLogger(LogImplSpec::class)

            log.log(LogLevel.INFO, "hello")

            // Dispatchers.Unconfined runs inline, so entries should be recorded immediately
            appender1.entries.size shouldBe 1
            appender2.entries.size shouldBe 1

            appender1.entries[0].level shouldBe LogLevel.INFO
            appender1.entries[0].message shouldBe "hello"
            appender1.entries[0].loggerName shouldBe "io.peekandpoke.ultra.log.LogImplSpec"
        }

        "LogImpl convenience methods use correct log levels" {
            val appender = RecordingAppender()
            val manager = UltraLogManager(listOf(appender))
            val log = manager.getLogger(LogImplSpec::class)

            log.error("err")
            log.warning("warn")
            log.info("inf")
            log.debug("dbg")
            log.trace("trc")

            appender.entries.size shouldBe 5
            appender.entries[0].level shouldBe LogLevel.ERROR
            appender.entries[0].message shouldBe "err"
            appender.entries[1].level shouldBe LogLevel.WARNING
            appender.entries[1].message shouldBe "warn"
            appender.entries[2].level shouldBe LogLevel.INFO
            appender.entries[2].message shouldBe "inf"
            appender.entries[3].level shouldBe LogLevel.DEBUG
            appender.entries[3].message shouldBe "dbg"
            appender.entries[4].level shouldBe LogLevel.TRACE
            appender.entries[4].message shouldBe "trc"
        }

        "LogImpl uses caller's qualified name as loggerName" {
            val appender = RecordingAppender()
            val manager = UltraLogManager(listOf(appender))

            // Use a well-known class
            val log = manager.getLogger(String::class)
            log.info("test")

            appender.entries[0].loggerName shouldBe "kotlin.String"
        }

        "UltraLogManager.add registers appender for future messages" {
            val appender1 = RecordingAppender()
            val appender2 = RecordingAppender()
            val manager = UltraLogManager(listOf(appender1))
            val log = manager.getLogger(LogImplSpec::class)

            log.info("before add")
            appender1.entries.size shouldBe 1
            appender2.entries.size shouldBe 0

            manager.add(appender2)

            log.info("after add")
            appender1.entries.size shouldBe 2
            appender2.entries.size shouldBe 1
            appender2.entries[0].message shouldBe "after add"
        }

        "UltraLogManager with no appenders does not fail" {
            val manager = UltraLogManager(emptyList())
            val log = manager.getLogger(LogImplSpec::class)

            // Should not throw
            log.error("test")
            log.info("test")
        }
    }
}
