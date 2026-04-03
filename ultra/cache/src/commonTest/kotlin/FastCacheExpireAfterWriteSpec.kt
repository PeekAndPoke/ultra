package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class FastCacheExpireAfterWriteSpec : StringSpec() {

    init {
        "entries expire after ttl since write" {
            val ttl = 500.milliseconds

            val cache = fastCache<String, String> {
                expireAfterWrite(ttl)
            }

            cache.put("a", "1")
            cache.put("b", "2")

            // Wait past TTL
            delay(ttl * 1.5)

            cache.keys.shouldBeEmpty()
        }

        "reading an entry does NOT reset write ttl" {
            val ttl = 500.milliseconds

            val cache = fastCache<String, String> {
                expireAfterWrite(ttl)
            }

            cache.put("x", "v")

            // Keep reading within the TTL — should NOT extend lifetime
            delay(ttl * 0.4)
            cache.get("x") shouldBe "v"

            delay(ttl * 0.4)
            cache.get("x") shouldBe "v"

            // Now wait past the original write TTL
            delay(ttl * 0.5)

            cache.keys.shouldBeEmpty()
        }

        "putting an entry resets write ttl" {
            val ttl = 500.milliseconds

            val cache = fastCache<String, String> {
                expireAfterWrite(ttl)
            }

            cache.put("x", "v1")

            // Wait half TTL, then re-put
            delay(ttl * 0.5)
            cache.put("x", "v2")

            // Wait 3/4 TTL from the re-put — should still be alive
            delay(ttl * 0.75)
            cache.get("x") shouldBe "v2"

            // Wait past the re-put TTL
            delay(ttl * 0.75)
            cache.keys.shouldBeEmpty()
        }

        "removed entries are cleaned from behaviour tracking" {
            val ttl = 500.milliseconds

            val cache = fastCache<String, String> {
                expireAfterWrite(ttl)
            }

            cache.put("a", "1")
            cache.put("b", "2")
            cache.remove("a")

            // Wait past TTL — only b should be evicted, a was already removed
            delay(ttl * 1.5)

            cache.keys.shouldBeEmpty()
            cache.get("a") shouldBe null
            cache.get("b") shouldBe null
        }

        "expire after write with multiple entries, only expired ones are evicted" {
            val ttl = 800.milliseconds

            val cache = fastCache<String, String> {
                expireAfterWrite(ttl)
            }

            cache.put("a", "1")

            delay(ttl * 0.5)
            cache.put("b", "2")

            // Wait so that "a" expires but "b" does not
            delay(ttl * 0.75)

            cache.keys shouldContainExactlyInAnyOrder setOf("b")
        }
    }
}
