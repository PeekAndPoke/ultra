package io.peekandpoke.ultra.log

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Level filtering across both layers: the manager's global lower bound and each appender's own.
 */
class LogLevelFilteringSpec : StringSpec({

    class Recording(override val minLevel: LogLevel = LogLevel.ALL) : LogAppender {
        val events = CopyOnWriteArrayList<LogEvent>()
        override suspend fun append(event: LogEvent) {
            events.add(event)
        }

        val levels get() = events.map { it.level }
    }

    "OFF and ALL are thresholds, not event levels, and are never dispatched" {
        val rec = Recording()
        val log = UltraLogManager(listOf(rec)).getLogger(LogLevelFilteringSpec::class)

        log.log(LogLevel.OFF, "suppressed")
        log.log(LogLevel.ALL, "also suppressed")
        log.info("kept")

        rec.levels shouldContainExactly listOf(LogLevel.INFO)
    }

    "the global lower bound drops everything less critical" {
        val rec = Recording()
        val log = UltraLogManager(listOf(rec), minLevel = LogLevel.WARNING)
            .getLogger(LogLevelFilteringSpec::class)

        log.error("e")
        log.warning("w")
        log.info("i")
        log.debug("d")
        log.trace("t")

        rec.levels shouldContainExactly listOf(LogLevel.ERROR, LogLevel.WARNING)
    }

    "a per-appender bound drops for that appender only" {
        val everything = Recording(LogLevel.ALL)
        val errorsOnly = Recording(LogLevel.ERROR)
        val log = UltraLogManager(listOf(everything, errorsOnly))
            .getLogger(LogLevelFilteringSpec::class)

        log.error("e")
        log.info("i")

        everything.levels shouldContainExactly listOf(LogLevel.ERROR, LogLevel.INFO)
        errorsOnly.levels shouldContainExactly listOf(LogLevel.ERROR)
    }

    "isEnabled is false when no appender accepts the level" {
        val manager = UltraLogManager(listOf(Recording(LogLevel.ERROR)))

        manager.isEnabled(LogLevel.ERROR) shouldBe true
        manager.isEnabled(LogLevel.INFO) shouldBe false
        manager.isEnabled(LogLevel.OFF) shouldBe false
        manager.isEnabled(LogLevel.ALL) shouldBe false
    }

    "isEnabled is false when there are no appenders at all" {
        UltraLogManager(emptyList()).isEnabled(LogLevel.ERROR) shouldBe false
    }

    "a lambda message is not built when the level is not enabled" {
        val rec = Recording(LogLevel.ERROR)
        val log = UltraLogManager(listOf(rec)).getLogger(LogLevelFilteringSpec::class)

        var built = 0

        log.debug { built++; "expensive" }
        built shouldBe 0

        log.error { built++; "expensive" }
        built shouldBe 1

        rec.events.map { it.message } shouldContainExactly listOf("expensive")
    }

    "NullLog enables nothing, so a lambda message is never built" {
        var built = 0

        NullLog.info { built++; "expensive" }

        built shouldBe 0
        NullLog.isEnabled(LogLevel.ERROR) shouldBe false
    }

    "the Throwable travels on the event instead of being folded into the message" {
        val rec = Recording()
        val log = UltraLogManager(listOf(rec)).getLogger(LogLevelFilteringSpec::class)
        val boom = IllegalStateException("kaboom")

        log.error("failed", boom)

        val event = rec.events.single()
        event.error shouldBe boom
        event.message shouldBe "failed"
    }

    "an event without an error carries null" {
        val rec = Recording()
        UltraLogManager(listOf(rec)).getLogger(LogLevelFilteringSpec::class).info("plain")

        rec.events.single().error shouldBe null
    }

    "a filtered-out event reaches no appender at all" {
        val rec = Recording(LogLevel.ERROR)
        UltraLogManager(listOf(rec)).getLogger(LogLevelFilteringSpec::class).trace("nope")

        rec.events.shouldBeEmpty()
    }
})
