package io.peekandpoke.ultra.cache

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

/**
 * A [NullableCache.getOrPutAsync] provider that reaches a key being resolved further up its own call
 * chain waited on itself for good. It now fails that call. Each scenario runs under a timeout, so a
 * hang shows as a failure rather than a stuck run.
 */
class NullableCacheCycleSpec : StringSpec({

    "a provider resolving its own key fails instead of waiting on itself" {
        val cache = NullableCache<String, String>()

        withTimeout(2.seconds) {
            shouldThrow<IllegalStateException> {
                cache.getOrPutAsync("a") { cache.getOrPutAsync("a") { "inner" } }
            }
        }

        // nothing is left claimed: the key resolves normally afterwards
        withTimeout(2.seconds) {
            cache.getOrPutAsync("a") { "fine" } shouldBe "fine"
        }
    }

    "a cycle through another key (a -> b -> a) fails instead of hanging" {
        val cache = NullableCache<String, String>()

        withTimeout(2.seconds) {
            shouldThrow<IllegalStateException> {
                cache.getOrPutAsync("a") {
                    cache.getOrPutAsync("b") {
                        cache.getOrPutAsync("a") { "a again" }
                    }
                }
            }
        }

        cache.has("a") shouldBe false
        cache.has("b") shouldBe false
    }

    "the same key resolved through another cache inside a provider is no cycle" {
        val outer = NullableCache<String, String>()
        val inner = NullableCache<String, String>()

        withTimeout(2.seconds) {
            outer.getOrPutAsync("a") {
                "outer+" + inner.getOrPutAsync("a") { "inner" }
            } shouldBe "outer+inner"
        }
    }

    "nested resolution of other keys still works" {
        val cache = NullableCache<String, String>()

        withTimeout(2.seconds) {
            cache.getOrPutAsync("a") {
                val b = cache.getOrPutAsync("b") { "b" }
                "a+$b"
            } shouldBe "a+b"
        }
    }
})
