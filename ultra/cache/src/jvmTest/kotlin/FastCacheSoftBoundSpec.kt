package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.random.Random

/**
 * `maxEntries` and `maxMemoryUsage` are soft bounds, by decision (ULTRA-00159 round 3; the maintainer:
 * "Since this cache is called FastCache it is ok, when it is not 100% accurate").
 *
 * Both spare a key that was used after the batch they decided on was taken, so a key read in every
 * maintenance pass is never evicted by them, and the cache holds up to the number of distinct keys
 * touched within one pass. These tests pin that documented behaviour; the deterministic sparing rule is
 * pinned in FastCacheEvictionDeclineSpec.
 *
 * JVM only: it needs readers on other threads. On JS no action can be pending while `process()` runs
 * (the swap and the evictions run in one task), so there the bound is met in the next pass.
 */
class FastCacheSoftBoundSpec : StringSpec({

    "maxEntries(1) is a soft bound: two keys read in every pass both stay" {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val cache = fastCache<String, String>(scope = scope) { maxEntries(1) }
        val stop = AtomicBoolean(false)
        val missingKeys = List(1_000) { "miss-$it" }

        cache.put("a", "1")

        val readers = listOf(
            thread(isDaemon = true) {
                val random = Random(1)
                while (!stop.get()) cache.get(missingKeys[random.nextInt(missingKeys.size)])
            },
            thread(isDaemon = true) {
                while (!stop.get()) {
                    cache.get("a")
                    cache.get("b")
                    Thread.yield()
                }
            },
        )

        try {
            cache.put("b", "2")
            Thread.sleep(1_000)

            cache.size shouldBe 2
        } finally {
            stop.set(true)
            readers.forEach { it.join() }
            scope.cancel()
        }
    }

    "maxEntries(10) is a soft bound: a pool of 1 000 keys read through getOrPut all stay" {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val cache = fastCache<String, String>(scope = scope) { maxEntries(10) }
        val stop = AtomicBoolean(false)
        val pool = List(1_000) { "key-$it" }

        val reader = thread(isDaemon = true) {
            val random = Random(2)
            while (!stop.get()) {
                val key = pool[random.nextInt(pool.size)]
                cache.getOrPut(key) { key }
            }
        }

        try {
            Thread.sleep(1_000)

            // seen: all 1 000, against a bound of 10
            cache.size shouldBeGreaterThan 500
        } finally {
            stop.set(true)
            reader.join()
            scope.cancel()
        }
    }
})
