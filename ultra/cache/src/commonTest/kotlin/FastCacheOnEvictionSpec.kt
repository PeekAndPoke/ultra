package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class FastCacheOnEvictionSpec : StringSpec() {

    init {
        "onEviction fires when expireAfterAccess evicts" {
            val evicted = mutableListOf<Pair<String, String>>()

            val cache = fastCache<String, String> {
                onEviction { key, value -> evicted.add(key to value) }
                expireAfterAccess(500.milliseconds)
            }

            cache.put("a", "1")
            cache.put("b", "2")

            // Wait for eviction
            delay(800.milliseconds)

            evicted shouldContainExactlyInAnyOrder listOf("a" to "1", "b" to "2")
        }

        "onEviction does NOT fire on explicit remove()" {
            val evicted = mutableListOf<Pair<String, String>>()

            val cache = fastCache<String, String> {
                onEviction { key, value -> evicted.add(key to value) }
                expireAfterAccess(5_000.milliseconds)
            }

            cache.put("a", "1")
            // Let the loop process the put
            delay(100.milliseconds)

            cache.remove("a")
            // Let the loop process the remove
            delay(100.milliseconds)

            evicted.size shouldBe 0
        }

        "onEviction fires for each evicted entry with maxEntries" {
            val evicted = mutableListOf<Pair<String, String>>()

            val cache = fastCache<String, String> {
                onEviction { key, value -> evicted.add(key to value) }
                maxEntries(2)
            }

            cache.put("a", "1")
            cache.put("b", "2")
            cache.put("c", "3")

            // Let the loop process and evict
            delay(200.milliseconds)

            evicted.size shouldBe 1
            // "a" was the least recently accessed
            evicted[0].first shouldBe "a"
        }

        "onEviction receives the correct value" {
            val evicted = mutableListOf<Pair<String, String>>()

            val cache = fastCache<String, String> {
                onEviction { key, value -> evicted.add(key to value) }
                expireAfterAccess(300.milliseconds)
            }

            cache.put("x", "hello")
            delay(500.milliseconds)

            evicted.size shouldBe 1
            evicted[0] shouldBe ("x" to "hello")
        }
    }
}
