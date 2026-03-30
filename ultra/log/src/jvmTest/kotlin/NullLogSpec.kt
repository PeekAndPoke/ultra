package io.peekandpoke.ultra.log

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldBeInstanceOf

class NullLogSpec : StringSpec() {

    init {

        "NullLog implements Log" {
            NullLog.shouldBeInstanceOf<Log>()
        }

        "NullLog.log does not throw for any level" {
            LogLevel.entries.forEach { level ->
                NullLog.log(level, "test message at $level")
            }
        }

        "NullLog convenience methods do not throw" {
            NullLog.error("error message")
            NullLog.warning("warning message")
            NullLog.info("info message")
            NullLog.debug("debug message")
            NullLog.trace("trace message")
        }
    }
}
