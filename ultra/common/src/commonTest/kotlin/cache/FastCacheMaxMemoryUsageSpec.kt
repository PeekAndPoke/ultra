// language: kotlin
package de.peekandpoke.ultra.common.cache

import io.kotest.assertions.fail
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

/**
 * Tests for FastCache memory-limit behaviour (MaxMemoryUsageBehaviour).
 *
 * These tests use the normal default coroutine scope (Cache.defaultCoroutineScope) so the
 * FastCache internal processing loop runs on the real dispatcher. We wait a short while
 * for the loop to pick up the actions and perform eviction.
 */
class FastCacheMaxMemoryUsageSpec : StringSpec() {
    init {
        "max memory usage evicts oldest entries until under limit" {
            // Stub estimator that returns deterministic sizes for keys and values.
            class StubEstimator(private val sizes: Map<Any?, Long>) : ObjectSizeEstimator {
                override fun estimate(obj: Any?): Long {
                    return sizes[obj] ?: 1L
                }
            }

            // Sizes: key sizes are small, value sizes are chosen so each entry total = 50
            val sizes = mapOf<Any?, Long>(
                "k1" to 1L,
                "k2" to 1L,
                "k3" to 1L,
                "v1" to 49L,
                "v2" to 49L,
                "v3" to 49L,
            )

            val estimator = StubEstimator(sizes)

            // Use a small memory limit: 100 -> three entries of 50 each = 150 > 100 -> one eviction expected
            val cache = fastCache<Any, String> {
                // builder config - use the real default scope via fastCache default
                maxMemoryUsage(100L, estimator)
            }

            // Insert three entries. Use runBlocking to allow suspending delay below.
            cache.put("k1", "v1")
            // small delay between puts to make insertion order/time explicit (not strictly necessary)
            delay(10.milliseconds)
            cache.put("k2", "v2")
            delay(10.milliseconds)
            cache.put("k3", "v3")

            // Give the internal processing loop some time to run and evict
            // loopDelay is 100ms by default; wait a bit longer to be safe
            delay(500.milliseconds)

            // After eviction we expect only two entries left (total <= 100).
            // The eviction policy evicts the least-recently-accessed entries (tie-broken by insertion order).
            // Since we inserted k1, k2, k3 in that order, k1 should be evicted first.
            cache.keys shouldContainExactlyInAnyOrder listOf("k2", "k3")
        }

        "putting same key updates tracked size and does not double count" {
            class StubEstimator(private val sizes: Map<Any?, Long>) : ObjectSizeEstimator {
                override fun estimate(obj: Any?): Long {
                    return sizes[obj] ?: 1L
                }
            }

            // make each entry size 60; limit 100 -> only one entry fits
            val sizes = mapOf<Any?, Long>(
                "ka" to 1L,
                "kb" to 1L,
                "va1" to 59L,
                "va2" to 59L,
                "vb" to 59L,
            )
            val estimator = StubEstimator(sizes)

            val cache = fastCache<String, String> {
                maxMemoryUsage(100L, estimator)
            }

            // Put an entry "a" then overwrite it with a new value (both large)
            cache.put("ka", "va1")
            delay(10.milliseconds)
            cache.put("kb", "vb")
            delay(10.milliseconds)

            // Now overwrite "ka" with another large value - total size should still be two entries,
            // but the estimator must update totalSize correctly (not double count the key).
            cache.put("ka", "va2")

            // wait for loop
            delay(500.milliseconds)

            // Only one entry can remain because each entry ~60 and limit is 100.
            // The eviction should remove the least-recently-accessed; since kb was put before the final put(ka),
            // it's likely kb gets evicted leaving only ka, but we just assert that exactly one entry remains
            cache.entries.size shouldBe 1
        }

        "MaxMemoryUsageBehaviour.totalSize equals sum of sizes of entries tracked" {
            // A deterministic estimator returning exact sizes for given keys/values
            class StubEstimator(private val sizes: Map<Any?, Long>) : ObjectSizeEstimator {
                override fun estimate(obj: Any?): Long = sizes[obj] ?: 0L
            }

            val sizes = mapOf<Any?, Long>(
                // key sizes
                "k1" to 1L,
                "k2" to 1L,
                "k3" to 1L,
                // value sizes
                "v1" to 49L,
                "v2" to 79L,
                "v3" to 11L,
            )

            val estimator = StubEstimator(sizes)

            // Build cache with the behaviour and a small loopDelay so the loop runs frequently.
            val cache = fastCache<Any, Any>(loopDelay = 20.milliseconds) {
                maxMemoryUsage(10_000L, estimator) // large max to avoid eviction in this test
            }

            // Put three entries with different sizes
            cache.put("k1", "v1") // size = 1 + 49 = 50
            delay(10.milliseconds)
            cache.put("k2", "v2") // size = 1 + 79 = 80
            delay(10.milliseconds)
            cache.put("k3", "v3") // size = 1 + 11 = 12

            // Give the processing loop time to run and update the behaviour internal state
            delay(200.milliseconds)

            // Find the MaxMemoryUsageBehaviour instance exposed on the cache
            val behaviour =
                cache.behaviours.filterIsInstance<FastCache.MaxMemoryUsageBehaviour<Any, Any>>().firstOrNull()
                    ?: error("MaxMemoryUsageBehaviour not found on cache")

            // Expected total size = 50 + 80 + 12 = 142
            behaviour.totalSize shouldBe (50L + 80L + 12L)
        }

        "totalSize updates correctly when an entry is overwritten and when removed" {
            class StubEstimator(private val sizes: Map<Any?, Long>) : ObjectSizeEstimator {
                override fun estimate(obj: Any?): Long = sizes[obj] ?: 0L
            }

            val sizes = mapOf<Any?, Long>(
                "k" to 1L,
                "v1" to 20L,
                "v2" to 30L,
            )

            val estimator = StubEstimator(sizes)

            val cache = fastCache<Any, Any>(loopDelay = 20.milliseconds) {
                maxMemoryUsage(10_000L, estimator)
            }

            // Put initial value
            cache.put("k", "v1") // size = 1 + 20 = 21
            delay(100.milliseconds)

            val behaviour =
                cache.behaviours.filterIsInstance<FastCache.MaxMemoryUsageBehaviour<Any, Any>>().firstOrNull()
                    ?: error("MaxMemoryUsageBehaviour not found on cache")

            behaviour.totalSize shouldBe 21L

            // Overwrite same key with a larger value -> size should become 1 + 30 = 31
            cache.put("k", "v2")
            delay(100.milliseconds)

            behaviour.totalSize shouldBe 31L

            // Remove the key -> totalSize becomes 0
            cache.remove("k")
            delay(100.milliseconds)

            behaviour.totalSize shouldBe 0L
        }

        "MaxMemoryUsageBehaviour.totalSize must match total size ... randomized test" {
            // We estimate the size as the int input value itself.
            class StubEstimator() : ObjectSizeEstimator {
                override fun estimate(obj: Any?): Long = (obj as? Int)?.toLong() ?: 0L
            }

            val estimator = StubEstimator()

            val cache = fastCache<Int, Int> {
                maxMemoryUsage(1000L, estimator)
            }

            val behaviour = cache.behaviours
                .filterIsInstance<FastCache.MaxMemoryUsageBehaviour<Int, Int>>().first()

            val rand = Random(42)

            // 100 random rounds
            repeat(100) {

                // insert 100 numbers
                repeat(100) {
                    val num = rand.nextInt(100)
                    cache.put(num, num)
                }

                // Wait for the cache to settle down / inner loop to catch up
                delay(500.milliseconds)

                val expectedTotalMemoryUsage = (cache.keys.sum() + cache.values.sum()).toLong()

                if (behaviour.totalSize != expectedTotalMemoryUsage) {
                    fail("totalSize is ${behaviour.totalSize} but should be $expectedTotalMemoryUsage")
                }
            }
        }
    }
}
