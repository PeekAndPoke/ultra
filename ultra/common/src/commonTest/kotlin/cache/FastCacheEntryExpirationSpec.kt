// language: kotlin
package de.peekandpoke.ultra.common.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class FastCacheExpireAfterAccessSpec : StringSpec() {

    init {
        "entries expire after ttl since last access" {
            // Use a small TTL so the test is fast
            val ttl = 500.milliseconds

            val cache = fastCache<String, String> {
                expireAfterAccess(ttl)
            }

            // Put two entries
            cache.put("a", "1")
            cache.put("b", "2")

            // Wait half ttl again
            delay(ttl * 0.5)
            // Touch "a" so it stays fresh
            cache.get("a") shouldBe "1"

            // Wait 3/4 ttl again
            delay(ttl * 0.75)
            // Only a must be left, b was evicted
            cache.keys shouldContainExactlyInAnyOrder setOf("a")
        }

        "accessing an entry resets its ttl" {
            val ttl = 500.milliseconds

            val cache = fastCache<String, String> {
                expireAfterAccess(ttl)
            }

            cache.put("x", "v")

            // Wait slightly less than ttl, then access -> should extend lifetime
            // sleep a bit
            delay(ttl * 0.5)
            // Access the entry to refresh last-access
            cache.get("x") shouldBe "v"

            // Wait halt ttl
            delay(ttl * 0.5)
            // Key must still be there
            cache.keys shouldContainExactlyInAnyOrder setOf("x")

            // Wait 3 / 4 ttl again
            delay(ttl * 0.75)
            // At some point the key must be evicted
            cache.keys.shouldBeEmpty()
        }
    }
}
