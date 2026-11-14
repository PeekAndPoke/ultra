package io.peekandpoke.ultra.cache

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Failures are counted where they happen and reported by the processing loop, one line per kind of
 * failure and pass, outside the cache's lock - not one line per failing item from inside it.
 */
class FastCacheFailureReportSpec : StringSpec({

    timeout = TEST_TIMEOUT_MS

    "a listener failing for 1 000 evicted entries is reported by count, not line by line" {
        val original = System.out
        val buffer = ByteArrayOutputStream()
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val line = Regex("FastCache eviction listener failed (\\d+) time\\(s\\): IllegalStateException")

        System.setOut(PrintStream(buffer, true))

        try {
            // A loop that only drains, once a second; the tap shows when it did
            val tap = ActionTap<String, String>()
            val cache = fastCache<String, String>(scope = scope, loopDelay = 1.seconds) {
                addBehaviour(tap)
                onEviction { _, _ -> error("listener failed") }
            }
            val behaviour = FastCache.ExpireAfterWriteBehaviour<String, String>(ttl = 1.milliseconds)
            val keys = List(1_000) { "k$it" }

            keys.forEach { cache.put(it, it) }
            eventually(10.seconds) { (tap.actions.size >= keys.size) shouldBe true }

            // Driven by hand right after a drain, so no put is pending: 1 000 evictions, each listener
            // call fails
            behaviour.process(cache, FastCache.ActionUpdates(keys.map { FastCache.PutAction(it, it) }))
            Thread.sleep(5)
            behaviour.process(cache, FastCache.ActionUpdates(emptyList()))

            cache.size shouldBe 0

            // Reported by the loop at its next pass(es). A report can fall in the middle of the 1 000
            // evictions, which splits the count over two lines - never one line per failure.
            eventually(10.seconds) {
                val counts = buffer.toString().lines().mapNotNull { line.find(it)?.groupValues?.get(1)?.toInt() }

                counts.sum() shouldBe 1_000
                counts.size shouldBeLessThanOrEqual 2
            }

            keepReachable(cache)
        } finally {
            System.setOut(original)
            scope.cancel()
        }
    }
})
