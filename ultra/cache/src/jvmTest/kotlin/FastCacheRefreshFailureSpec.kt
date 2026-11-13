package io.peekandpoke.ultra.cache

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * How a failing refresh loader is handled.
 *
 * JVM only: an `Error` escaping a coroutine reaches the platform's last-resort handler, which on
 * native ends the process.
 */
class FastCacheRefreshFailureSpec : StringSpec({

    "a loader throwing an Error releases its key for the next refresh" {
        val loads = AtomicInteger(0)

        val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
            refreshAfterWrite(50.milliseconds) {
                if (loads.incrementAndGet() == 1) throw AssertionError("first load fails with an Error")
                it
            }
        }

        cache.put("a", "1")

        eventually(3.seconds) {
            loads.get() shouldBeGreaterThanOrEqual 2
        }
    }
})
