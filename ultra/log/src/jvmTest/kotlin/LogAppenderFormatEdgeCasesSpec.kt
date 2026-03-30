package io.peekandpoke.ultra.log

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.time.ZonedDateTime

class LogAppenderFormatEdgeCasesSpec : StringSpec() {

    private val ts = ZonedDateTime.parse("2025-01-15T08:30:00+01:00[Europe/Berlin]")

    init {

        "format uses short logger name as-is when length <= 30" {
            val shortName = "a.b.Logger" // 10 chars
            val result = LogAppender.format(ts, LogLevel.INFO, "msg", shortName)

            result shouldContain "a.b.Logger"
        }

        "format abbreviates logger name when length > 30" {
            val longName = "io.peekandpoke.ultra.log.service.MyService" // 43 chars
            val result = LogAppender.format(ts, LogLevel.ERROR, "msg", longName)

            result shouldContain "i.p.u.l.s.MyService"
            result shouldNotContain longName
        }

        "format includes exact 30-char name without abbreviation" {
            // Build a name exactly 30 chars long
            val exactName = "a".repeat(30) // 30 chars
            val result = LogAppender.format(ts, LogLevel.DEBUG, "msg", exactName)

            result shouldContain exactName
        }

        "format with 31-char name abbreviates" {
            // "io.peekandpoke.ultra.logging.Xx" is 31 chars -> triggers abbreviation
            val longishName = "io.peekandpoke.ultra.logging.Xx"
            val result = LogAppender.format(ts, LogLevel.DEBUG, "msg", longishName)

            result shouldContain "i.p.u.l.Xx"
        }

        "formatLoggerName with empty segments" {
            // Edge case: dots with single-char segments
            val name = "a.b.c.D"
            val result = LogAppender.formatLoggerName(name)

            result shouldBe "a.b.c.D"
        }

        "formatLoggerName with deeply nested package" {
            val name = "com.example.very.deep.nested.package.structure.MyClass"
            val result = LogAppender.formatLoggerName(name)

            result shouldBe "c.e.v.d.n.p.s.MyClass"
        }

        "formatLoggerName results are cached" {
            val name = "io.peekandpoke.cache.test.CacheSpec"

            // Call twice and verify same result (cache should work transparently)
            val first = LogAppender.formatLoggerName(name)
            val second = LogAppender.formatLoggerName(name)

            first shouldBe second
            first shouldBe "i.p.c.t.CacheSpec"
        }

        "format with empty message" {
            val result = LogAppender.format(ts, LogLevel.WARNING, "", "short.Logger")

            result shouldContain "WARNING"
            result shouldContain "short.Logger"
        }

        "format with all log levels" {
            val levels = listOf(
                LogLevel.OFF to "OFF",
                LogLevel.ERROR to "ERROR",
                LogLevel.WARNING to "WARNING",
                LogLevel.INFO to "INFO",
                LogLevel.DEBUG to "DEBUG",
                LogLevel.TRACE to "TRACE",
                LogLevel.ALL to "ALL",
            )

            levels.forEach { (level, expected) ->
                val result = LogAppender.format(ts, level, "test", "short.L")
                result shouldContain expected
            }
        }

        "format output structure is: date time LEVEL - name - message" {
            val result = LogAppender.format(ts, LogLevel.INFO, "hello world", "my.Logger")

            // toLocalDate() = "2025-01-15", toLocalTime() = "08:30"
            result shouldBe "2025-01-15 08:30 INFO - my.Logger - hello world"
        }

        "format preserves message with special characters" {
            val msg = "Error: NullPointerException at line 42 [code=500]"
            val result = LogAppender.format(ts, LogLevel.ERROR, msg, "short.L")

            result shouldContain msg
        }
    }
}
