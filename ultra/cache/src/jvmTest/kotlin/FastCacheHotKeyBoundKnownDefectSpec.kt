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
 * KNOWN DEFECT, asserted as it is today (ULTRA-00159 review round 3, the MAJOR): `maxEntries` and
 * `maxMemoryUsage` do not bind for keys that are read in every pass.
 *
 * Both evict through `removeSilently` with "any pending action spares the key". A key read between the
 * loop's swap and its eviction is spared, and so in every pass. Seen 5 of 5 runs for each test.
 *
 * These tests pass while the defect is there. A fix makes them fail: then turn them into tests that the
 * bound holds, and drop "KNOWN DEFECT" from the names.
 *
 * JVM only: it needs readers on other threads. On JS no action can be pending while `process()` runs
 * (the swap and the evictions run in one task), so the bound holds there.
 */
class FastCacheHotKeyBoundKnownDefectSpec : StringSpec({

    "KNOWN DEFECT: maxEntries(1) does not bind for two keys read in every pass" {
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
            Thread.sleep(2_000)

            cache.size shouldBe 2
        } finally {
            stop.set(true)
            readers.forEach { it.join() }
            scope.cancel()
        }
    }

    "KNOWN DEFECT: maxEntries(10) does not bind for a pool of 1 000 keys read through getOrPut" {
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
            Thread.sleep(2_000)

            // seen: all 1 000, against a bound of 10
            cache.size shouldBeGreaterThan 500
        } finally {
            stop.set(true)
            reader.join()
            scope.cancel()
        }
    }
})
