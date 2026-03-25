package io.peekandpoke.ultra.log

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.ZonedDateTime

class LogAppenderSpec : StringSpec({

    "formatLoggerName shortens package parts to first char" {
        LogAppender.formatLoggerName("io.peekandpoke.ultra.log.MyLogger") shouldBe "i.p.u.l.MyLogger"
    }

    "formatLoggerName keeps single-part names unchanged" {
        LogAppender.formatLoggerName("MyLogger") shouldBe "MyLogger"
    }

    "formatLoggerName with two parts" {
        LogAppender.formatLoggerName("app.Service") shouldBe "a.Service"
    }

    "format includes all components" {
        val ts = ZonedDateTime.parse("2024-06-15T10:30:45+02:00[Europe/Berlin]")
        val result = LogAppender.format(ts, LogLevel.INFO, "hello world", "short.Logger")

        result shouldContain "2024-06-15"
        result shouldContain "10:30:45"
        result shouldContain "INFO"
        result shouldContain "short.Logger"
        result shouldContain "hello world"
    }

    "format shortens long logger names" {
        val ts = ZonedDateTime.parse("2024-06-15T10:30:45+02:00[Europe/Berlin]")
        val longName = "io.peekandpoke.ultra.some.very.deep.package.MyService"

        val result = LogAppender.format(ts, LogLevel.DEBUG, "msg", longName)

        // Should use abbreviated name since > 30 chars
        result shouldContain "i.p.u.s.v.d.p.MyService"
    }
})
