package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.time.Duration.Companion.milliseconds

/**
 * Failures are counted where they happen and reported by the processing loop, one line per kind of
 * failure and pass, outside the cache's lock - not one line per failing item from inside it.
 */
class FastCacheFailureReportSpec : StringSpec({

    "a listener failing for 1 000 evicted entries gives one line with the count" {
        val original = System.out
        val buffer = ByteArrayOutputStream()
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        System.setOut(PrintStream(buffer, true))

        try {
            val cache = fastCache<String, String>(scope = scope, loopDelay = 50.milliseconds) {
                onEviction { _, _ -> error("listener failed") }
            }
            val behaviour = FastCache.ExpireAfterWriteBehaviour<String, String>(ttl = 1.milliseconds)
            val keys = List(1_000) { "k$it" }

            keys.forEach { cache.put(it, it) }
            delay(150.milliseconds)

            // one pass, driven by hand: 1 000 evictions, each listener call fails
            behaviour.process(cache, FastCache.ActionUpdates(keys.map { FastCache.PutAction(it, it) }))
            delay(20.milliseconds)
            behaviour.process(cache, FastCache.ActionUpdates(emptyList()))

            cache.size shouldBe 0

            // the loop reports at its next pass
            delay(200.milliseconds)
            keepReachable(cache)
        } finally {
            System.setOut(original)
            scope.cancel()
        }

        val lines = buffer.toString().lines().filter { it.contains("eviction listener") }

        lines.size shouldBe 1
        lines.single() shouldContain "1000"
        lines.single() shouldContain "IllegalStateException"
    }
})
