package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class FastCacheStatisticsSpec : StringSpec() {

    init {
        "statistics tracks hits and misses" {
            lateinit var stats: FastCache.StatisticsBehaviour<String, String>

            val cache = fastCache<String, String> {
                stats = statistics()
            }

            cache.put("a", "1")
            cache.get("a") shouldBe "1"  // hit
            cache.get("missing")         // miss

            // Let the loop process
            delay(200.milliseconds)

            val snapshot = stats.snapshot()
            snapshot.putCount shouldBe 1
            snapshot.hitCount shouldBe 1
            snapshot.missCount shouldBe 1
        }

        "statistics tracks put count" {
            lateinit var stats: FastCache.StatisticsBehaviour<String, String>

            val cache = fastCache<String, String> {
                stats = statistics()
            }

            cache.put("a", "1")
            cache.put("b", "2")
            cache.put("a", "3")  // overwrite

            delay(100.milliseconds)

            stats.snapshot().putCount shouldBe 3
        }

        "statistics tracks eviction count" {
            lateinit var stats: FastCache.StatisticsBehaviour<String, String>

            val cache = fastCache<String, String> {
                stats = statistics()
                maxEntries(2)
            }

            cache.put("a", "1")
            cache.put("b", "2")
            cache.put("c", "3")  // triggers eviction of "a"

            delay(200.milliseconds)

            stats.snapshot().evictionCount shouldBe 1
        }

        "statistics hitRate calculation" {
            lateinit var stats: FastCache.StatisticsBehaviour<String, String>

            val cache = fastCache<String, String> {
                stats = statistics()
            }

            cache.put("a", "1")
            cache.get("a")        // hit
            cache.get("a")        // hit
            cache.get("a")        // hit
            cache.get("missing")  // miss

            delay(100.milliseconds)

            val snapshot = stats.snapshot()
            snapshot.requestCount shouldBe 4
            snapshot.hitRate shouldBe 0.75
        }

        "statistics snapshot is immutable - new operations do not change old snapshot" {
            lateinit var stats: FastCache.StatisticsBehaviour<String, String>

            val cache = fastCache<String, String> {
                stats = statistics()
            }

            cache.put("a", "1")
            cache.get("a")  // hit

            delay(100.milliseconds)

            val before = stats.snapshot()
            before.hitCount shouldBe 1

            // More operations
            cache.get("a")  // another hit
            delay(100.milliseconds)

            // Old snapshot unchanged
            before.hitCount shouldBe 1

            // New snapshot reflects updates
            stats.snapshot().hitCount shouldBe 2
        }

        "has() counts as hit when key exists, miss when absent" {
            lateinit var stats: FastCache.StatisticsBehaviour<String, String>

            val cache = fastCache<String, String> {
                stats = statistics()
            }

            cache.put("a", "1")
            cache.has("a")        // hit
            cache.has("missing")  // miss

            delay(100.milliseconds)

            val snapshot = stats.snapshot()
            snapshot.hitCount shouldBe 1
            snapshot.missCount shouldBe 1
        }
    }
}
