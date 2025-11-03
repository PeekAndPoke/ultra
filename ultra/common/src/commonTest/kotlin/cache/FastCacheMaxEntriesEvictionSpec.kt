// language: kotlin
package de.peekandpoke.ultra.common.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class FastCacheMaxEntriesSpec : StringSpec({

    "maxEntries evicts oldest inserted entries until size <= maxEntries" {
        // Build a cache with maxEntries = 2
        val cache = fastCache<String, String> {
            maxEntries(2)
        }

        // Insert three entries in order
        cache.put("one", "1")

        delay(10.milliseconds)
        cache.put("two", "2")

        delay(10.milliseconds)
        cache.put("three", "3")

        // Give the internal loop time to process eviction (loopDelay default is small)
        delay(300.milliseconds)

        // The oldest inserted entry ("one") should have been evicted; two newest remain
        cache.keys shouldContainExactlyInAnyOrder listOf("two", "three")
    }

    "maxEntries does nothing when size <= maxEntries" {
        val cache = fastCache<String, String> {
            maxEntries(3)
        }

        cache.put("a", "A")
        cache.put("b", "B")

        delay(200.milliseconds)

        // both entries must remain
        cache.keys shouldContainExactlyInAnyOrder listOf("a", "b")
        cache.entries.size shouldBe 2
    }

    "Reads affect MaxEntries eviction order (evicts by access order)" {
        // MaxEntriesBehaviour implementation evicts based on entries iteration order
        // (insertion order for the backing map), so reads shouldn't change which item is evicted.
        val cache = fastCache<String, String> {
            maxEntries(2)
        }

        cache.put("old", "v1")

        // Longer delay to wait for the inner loop to process
        delay(200.milliseconds)
        cache.put("mid", "v2")

        // Longer delay to wait for the inner loop to process
        delay(200.milliseconds)
        cache.put("new", "v3")

        // Access "old" and "mid" a bunch of times to simulate "frequent" reads
        repeat(5) { _ ->
            cache.get("old")
            delay(50.milliseconds)
        }

        // "old" should stay as it was touched
        // "new" should stay as it has a greater access timestamp than "mid"
        cache.keys shouldContainExactlyInAnyOrder listOf("old", "new")
    }
})
