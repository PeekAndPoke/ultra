package io.peekandpoke.ultra.log

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
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

    "an appender after a suspending one has the event before log returns" {
        // The insights per-request collector sits behind the Karango appender, which suspends on its
        // insert; a request that ended meanwhile lost the line. No waiting here: delivery must be eager.
        val trailing = Recording("trailing")
        val manager = UltraLogManager(listOf(Suspending(delayMs = 5_000), trailing))

        manager.getLogger(UltraLogManagerDispatchSpec::class).info("first")
        manager.getLogger(UltraLogManagerDispatchSpec::class).warning("second")

        trailing.entries shouldContainExactly listOf("first", "second")
    }

    "an appender registered during a dispatch does not break it" {
        // Deterministic: the appender at index 0 registers another one from inside `append`, i.e. while
        // `log` is still iterating. A plain list throws ConcurrentModificationException from the iterator's
        // next() - out of `log`, into the caller - and the recorder after it never gets the event.
        lateinit var manager: UltraLogManager
        val trailing = Recording("trailing")
        val registering = object : LogAppender {
            override suspend fun append(event: LogEvent) {
                manager.add(Recording("added-mid-dispatch"))
            }
        }
        manager = UltraLogManager(listOf(registering, trailing))

        val caught = runCatching {
            manager.getLogger(UltraLogManagerDispatchSpec::class).info("event")
        }.exceptionOrNull()

        caught shouldBe null
        trailing.entries shouldContainExactly listOf("event")
    }
})
