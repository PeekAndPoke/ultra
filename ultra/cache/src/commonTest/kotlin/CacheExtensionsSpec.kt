package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CacheExtensionsSpec : StringSpec() {
    init {
        "isEmpty returns true for empty cache" {
            val cache = fastCache<String, String>()

            with(Cache.Companion) {
                cache.isEmpty() shouldBe true
            }
        }

        "isEmpty returns false for non-empty cache" {
            val cache = fastCache<String, String>()
            cache.put("k", "v")

            with(Cache.Companion) {
                cache.isEmpty() shouldBe false
            }
        }

        "isNotEmpty returns false for empty cache" {
            val cache = fastCache<String, String>()

            with(Cache.Companion) {
                cache.isNotEmpty() shouldBe false
            }
        }

        "isNotEmpty returns true for non-empty cache" {
            val cache = fastCache<String, String>()
            cache.put("k", "v")

            with(Cache.Companion) {
                cache.isNotEmpty() shouldBe true
            }
        }

        "isEmpty and isNotEmpty update after clear" {
            val cache = fastCache<String, String>()
            cache.put("k", "v")

            with(Cache.Companion) {
                cache.isEmpty() shouldBe false
                cache.isNotEmpty() shouldBe true

                cache.clear()

                cache.isEmpty() shouldBe true
                cache.isNotEmpty() shouldBe false
            }
        }
    }
}
