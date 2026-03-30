package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class FastCacheClearSpec : StringSpec() {
    init {
        "clear removes all entries" {
            val cache = fastCache<String, String>()

            cache.put("a", "1")
            cache.put("b", "2")
            cache.put("c", "3")

            cache.size shouldBe 3

            cache.clear()

            cache.size shouldBe 0
            cache.keys.shouldBeEmpty()
            cache.values.shouldBeEmpty()
            cache.entries shouldBe emptyMap()
        }

        "clear on empty cache is a no-op" {
            val cache = fastCache<String, String>()

            cache.size shouldBe 0

            cache.clear()

            cache.size shouldBe 0
        }

        "cache is usable after clear" {
            val cache = fastCache<String, String>()

            cache.put("x", "old")
            cache.clear()

            cache.get("x").shouldBeNull()

            cache.put("x", "new")
            cache.get("x") shouldBe "new"
            cache.size shouldBe 1
        }
    }
}
