package de.peekandpoke.ultra.logging

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class LogLevelSpec : FreeSpec() {

    init {
        "Log levels must have the correct values" - {

            "LogLevel.OFF" {
                LogLevel.OFF.severity shouldBe Int.MAX_VALUE
            }

            "LogLevel.ERROR" {
                LogLevel.ERROR.severity shouldBe 500
            }

            "LogLevel.WARNING" {
                LogLevel.WARNING.severity shouldBe 400
            }

            "LogLevel.INFO" {
                LogLevel.INFO.severity shouldBe 300
            }

            "LogLevel.DEBUG" {
                LogLevel.DEBUG.severity shouldBe 200
            }

            "LogLevel.TRACE" {
                LogLevel.TRACE.severity shouldBe 100
            }

            "LogLevel.ALL" {
                LogLevel.ALL.severity shouldBe 0
            }
        }
    }
}
