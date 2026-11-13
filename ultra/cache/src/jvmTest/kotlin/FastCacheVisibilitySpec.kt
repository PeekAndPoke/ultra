package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.concurrent.thread

/**
 * Values the processing loop writes must become visible to other threads (F9).
 *
 * A reader thread spins on the value until it changes. Without a memory barrier the JIT may hoist
 * the field read out of the compiled loop, and the reader never sees the write: the join times out.
 * The spin is given time to be compiled before the write happens.
 *
 * Readers are daemon threads: should the value never become visible, one keeps spinning until the
 * test JVM exits, while the test fails.
 */
class FastCacheVisibilitySpec : StringSpec() {

    private fun spinUntil(condition: () -> Boolean, write: () -> Unit): Boolean {
        val reader = thread(isDaemon = true) {
            @Suppress("ControlFlowWithEmptyBody")
            while (!condition()) {
            }
        }

        // let the spin loop get compiled
        Thread.sleep(1_000)

        write()

        reader.join(5_000)

        return !reader.isAlive
    }

    init {
        "statistics written on another thread are seen by snapshot()" {
            val cache = fastCache<String, String> {}
            val behaviour = FastCache.StatisticsBehaviour<String, String>()

            val seen = spinUntil(
                condition = { behaviour.snapshot().hitCount > 0L },
                write = {
                    behaviour.process(cache, FastCache.ActionUpdates(listOf(FastCache.ReadAction("a", "1"))))
                },
            )

            seen shouldBe true
        }

        "totalSize written on another thread is seen by the getter" {
            val cache = fastCache<String, String> {}
            val behaviour = FastCache.MaxMemoryUsageBehaviour<String, String>(maxMemorySize = Long.MAX_VALUE)

            val seen = spinUntil(
                condition = { behaviour.totalSize > 0L },
                write = {
                    behaviour.process(cache, FastCache.ActionUpdates(listOf(FastCache.PutAction("a", "1"))))
                },
            )

            seen shouldBe true
        }
    }
}
