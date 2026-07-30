package io.peekandpoke.ultra.log

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.time.ZonedDateTime
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Dispatch robustness: one appender must not be able to silence the others, and registering an
 * appender must not lose an in-flight event.
 */
class UltraLogManagerDispatchSpec : StringSpec({

    class Recording(val name: String) : LogAppender {
        val entries = CopyOnWriteArrayList<String>()
        override suspend fun append(event: LogEvent) {
            entries.add(event.message)
        }
    }

    class Suspending(private val delayMs: Long) : LogAppender {
        override suspend fun append(event: LogEvent) {
            delay(delayMs)
        }
    }

    class Exploding : LogAppender {
        override suspend fun append(event: LogEvent) {
            throw IllegalStateException("appender exploded")
        }
    }

    "a throwing appender does not suppress the appenders after it" {
        val after = Recording("after")
        val manager = UltraLogManager(listOf(Exploding(), after))

        manager.getLogger(UltraLogManagerDispatchSpec::class).info("must still arrive")

        after.entries shouldContainExactly listOf("must still arrive")
    }

    "a throwing appender does not surface to the logging caller" {
        val manager = UltraLogManager(listOf(Exploding()))

        val caught = runCatching {
            manager.getLogger(UltraLogManagerDispatchSpec::class).error("boom")
        }.exceptionOrNull()

        caught shouldBe null
    }

    "an appender registered during an in-flight dispatch does not lose the event" {
        // Deterministic by construction, not by racing threads: `append` is a suspend function, so a
        // suspending appender holds the dispatch iteration open across its IO. `log` returns at that
        // suspension point, the test thread then mutates the list, and the iterator resumes into it.
        //
        // Two details make the naive version of this test vacuous. ArrayList's iterator checks
        // modCount in next(), not hasNext(), so an appender at index 0 always receives the event
        // before the throw - the recorder has to come AFTER the suspending one. And the CME lands
        // inside the dispatch coroutine, never on the caller, so asserting that `log.info` did not
        // throw proves nothing whatsoever. Only the delivery count is evidence.
        val slow = Suspending(delayMs = 50)
        val trailing = Recording("trailing")
        val manager = UltraLogManager(listOf(slow, trailing))

        manager.getLogger(UltraLogManagerDispatchSpec::class).info("event")

        manager.add(Recording("added-mid-flight"))

        // let the parked dispatch resume and finish
        withTimeout(5000) {
            while (trailing.entries.isEmpty()) {
                delay(10)
            }
        }

        trailing.entries shouldContainExactly listOf("event")
    }
})
