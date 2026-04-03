package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class FastCacheRefreshAfterWriteSpec : StringSpec() {

    init {
        "stale entry is refreshed after ttl" {
            var loadCount = 0

            val cache = fastCache<String, String> {
                refreshAfterWrite(ttl = 300.milliseconds) { key ->
                    loadCount++
                    "$key-refreshed-$loadCount"
                }
            }

            cache.put("a", "initial")

            // Wait for refresh TTL to pass + loop to process + refresh to complete
            delay(600.milliseconds)

            cache.get("a") shouldBe "a-refreshed-1"
            loadCount shouldBe 1
        }

        "stale value is served during refresh" {
            val cache = fastCache<String, String> {
                refreshAfterWrite(ttl = 200.milliseconds) { key ->
                    // Simulate slow loader
                    delay(500.milliseconds)
                    "$key-new"
                }
            }

            cache.put("a", "old")

            // Wait past refresh TTL but not long enough for loader to complete
            delay(400.milliseconds)

            // Stale value should still be served
            cache.get("a") shouldBe "old"

            // Wait for loader to complete
            delay(500.milliseconds)

            cache.get("a") shouldBe "a-new"
        }

        "only one refresh per key at a time" {
            var loadCount = 0

            val cache = fastCache<String, String>(loopDelay = 50.milliseconds) {
                refreshAfterWrite(ttl = 200.milliseconds) { key ->
                    loadCount++
                    delay(300.milliseconds)
                    "$key-v$loadCount"
                }
            }

            cache.put("a", "initial")

            // Wait long enough for multiple loop iterations past TTL
            delay(500.milliseconds)

            // Only one load should have been triggered
            loadCount shouldBe 1
        }

        "hard ttl evicts even during refresh" {
            val cache = fastCache<String, String> {
                refreshAfterWrite(
                    ttl = 200.milliseconds,
                    hardTtl = 400.milliseconds,
                ) { key ->
                    // Very slow loader — won't complete before hard TTL
                    delay(2_000.milliseconds)
                    "$key-new"
                }
            }

            cache.put("a", "old")

            // Wait past hard TTL
            delay(700.milliseconds)

            // Entry should be evicted
            cache.get("a") shouldBe null
            cache.keys.shouldBeEmpty()
        }

        "failed refresh allows retry on next iteration" {
            var attempt = 0

            val cache = fastCache<String, String>(loopDelay = 50.milliseconds) {
                refreshAfterWrite(ttl = 200.milliseconds) { key ->
                    attempt++
                    if (attempt == 1) {
                        error("first attempt fails")
                    }
                    "$key-refreshed"
                }
            }

            cache.put("a", "initial")

            // Wait for first refresh attempt to fail and second to succeed
            delay(800.milliseconds)

            // At least 2 attempts (first fails, second succeeds), may be more due to timing
            (attempt >= 2) shouldBe true
            cache.get("a") shouldBe "a-refreshed"
        }
    }
}
