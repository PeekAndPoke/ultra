package io.peekandpoke.ultra.log

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

class LogLevelOrderingSpec : StringSpec() {

    init {

        "severity ordering: OFF > ERROR > WARNING > INFO > DEBUG > TRACE > ALL" {
            assertSoftly {
                LogLevel.OFF.severity shouldBeGreaterThan LogLevel.ERROR.severity
                LogLevel.ERROR.severity shouldBeGreaterThan LogLevel.WARNING.severity
                LogLevel.WARNING.severity shouldBeGreaterThan LogLevel.INFO.severity
                LogLevel.INFO.severity shouldBeGreaterThan LogLevel.DEBUG.severity
                LogLevel.DEBUG.severity shouldBeGreaterThan LogLevel.TRACE.severity
                LogLevel.TRACE.severity shouldBeGreaterThan LogLevel.ALL.severity
            }
        }

        "ALL has severity 0" {
            LogLevel.ALL.severity shouldBe 0
        }

        "OFF has severity Int.MAX_VALUE" {
            LogLevel.OFF.severity shouldBe Int.MAX_VALUE
        }

        "entries sorted by severity descending matches expected order" {
            val expectedOrder = listOf(
                LogLevel.OFF,
                LogLevel.ERROR,
                LogLevel.WARNING,
                LogLevel.INFO,
                LogLevel.DEBUG,
                LogLevel.TRACE,
                LogLevel.ALL,
            )

            LogLevel.entries.sortedByDescending { it.severity } shouldBe expectedOrder
        }

        "all severity values are unique" {
            val severities = LogLevel.entries.map { it.severity }
            severities.distinct().size shouldBe severities.size
        }

        "all severity values are non-negative" {
            assertSoftly {
                LogLevel.entries.forEach { level ->
                    withClue("${level.name}.severity should be >= 0") {
                        (level.severity >= 0) shouldBe true
                    }
                }
            }
        }

        "LogLevel.entries contains exactly 7 values" {
            LogLevel.entries.size shouldBe 7
        }

        "LogLevel.valueOf works for all level names" {
            assertSoftly {
                LogLevel.entries.forEach { level ->
                    LogLevel.valueOf(level.name) shouldBe level
                }
            }
        }

        "filtering by severity comparison works for level-gating" {
            // Simulate a level gate: only log messages with severity >= INFO
            val gate = LogLevel.INFO

            val allowed = LogLevel.entries.filter { it.severity >= gate.severity }
            val blocked = LogLevel.entries.filter { it.severity < gate.severity }

            allowed shouldBe listOf(LogLevel.OFF, LogLevel.ERROR, LogLevel.WARNING, LogLevel.INFO)
            blocked shouldBe listOf(LogLevel.DEBUG, LogLevel.TRACE, LogLevel.ALL)
        }

        "severity can be used for threshold comparison" {
            // A common pattern: should we log at DEBUG when our threshold is WARNING?
            val threshold = LogLevel.WARNING
            val messageLevel = LogLevel.DEBUG

            // DEBUG (200) < WARNING (400), so this message should NOT pass
            (messageLevel.severity < threshold.severity) shouldBe true
        }
    }
}
