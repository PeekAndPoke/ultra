package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * A write TTL applies to a key that is read in every pass (ULTRA-00159 review round 2, M1).
 *
 * `removeSilently` used to decline an eviction while ANY action on the key was pending, reads
 * included. A key read between the loop's swap and the eviction was spared, and the retry next pass
 * met the same condition. A flood of misses makes each batch large, so processing it takes long and a
 * read of the hot key always lands in that window: 5 of 5 runs survived 10 s against a 100 ms TTL.
 *
 * Each cache gets a scope of its own, cancelled at the end: a loop left processing the flood of
 * misses kept the shared dispatcher busy and starved the next spec's timing.
 *
 * JVM only: it needs readers on other threads. On JS the window cannot open, `process()` runs without
 * a suspension point.
 */
class FastCacheHotKeySpec : StringSpec({

    /** Whether "a" leaves the cache within [millis], with one thread flooding misses and one reading "a". */
    fun expiresUnderHotReads(millis: Long, configure: FastCache.Builder<String, String>.() -> Unit): Boolean {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val cache = fastCache(scope = scope, configure = configure)
        val stop = AtomicBoolean(false)

        cache.put("a", "1")

        val readers = listOf(
            thread(isDaemon = true) {
                val random = Random(1)
                while (!stop.get()) cache.get("miss-" + random.nextInt(1_000_000))
            },
            thread(isDaemon = true) {
                while (!stop.get()) {
                    cache.get("a")
                    Thread.yield()
                }
            },
        )

        try {
            val deadline = System.currentTimeMillis() + millis

            while (System.currentTimeMillis() < deadline) {
                if (!cache.keys.contains("a")) return true
                Thread.sleep(5)
            }

            return !cache.keys.contains("a")
        } finally {
            stop.set(true)
            readers.forEach { it.join() }
            scope.cancel()
        }
    }

    "expireAfterWrite(100 ms) expires a key read in every pass" {
        expiresUnderHotReads(millis = 2_000) {
            expireAfterWrite(100.milliseconds)
        } shouldBe true
    }

    "the refresh hard TTL (100 ms) evicts a key read in every pass" {
        expiresUnderHotReads(millis = 2_000) {
            refreshAfterWrite(10.seconds, hardTtl = 100.milliseconds) { it }
        } shouldBe true
    }
})
