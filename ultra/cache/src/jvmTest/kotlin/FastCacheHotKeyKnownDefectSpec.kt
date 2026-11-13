package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * KNOWN DEFECT, asserted as it is today (ULTRA-00159 review round 2, M1): a write TTL never applies to
 * a key that is read in every pass.
 *
 * `removeSilently` declines an eviction while ANY action on the key is pending, reads included. A key
 * read between the loop's swap and the eviction is spared, and the retry next pass meets the same
 * condition. A flood of misses makes each batch large, so processing it takes long and a read of the
 * hot key always lands in that window. Seen 5 of 5 runs surviving 10 s against a 100 ms TTL.
 *
 * These tests pass while the defect is there. A fix makes them fail: then turn them into tests that
 * the entry is gone, and drop "KNOWN DEFECT" from the names.
 *
 * JVM only: it needs readers on other threads. On JS the window cannot open, `process()` runs without
 * a suspension point.
 */
class FastCacheHotKeyKnownDefectSpec : StringSpec({

    /** Whether "a" is still cached after [millis], with one thread flooding misses and one reading "a". */
    fun survivesHotReads(cache: FastCache<String, String>, millis: Long): Boolean {
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
            Thread.sleep(millis)

            return cache.keys.contains("a")
        } finally {
            stop.set(true)
            readers.forEach { it.join() }
        }
    }

    "KNOWN DEFECT M1: expireAfterWrite(100 ms) never expires a key read in every pass" {
        val cache = fastCache<String, String> { expireAfterWrite(100.milliseconds) }

        survivesHotReads(cache, millis = 2_000) shouldBe true
    }

    "KNOWN DEFECT M1: the refresh hard TTL (100 ms) never evicts a key read in every pass" {
        val cache = fastCache<String, String> {
            refreshAfterWrite(10.seconds, hardTtl = 100.milliseconds) { it }
        }

        survivesHotReads(cache, millis = 2_000) shouldBe true
    }
})
