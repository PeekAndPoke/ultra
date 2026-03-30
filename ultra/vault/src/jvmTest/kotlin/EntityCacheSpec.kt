package io.peekandpoke.ultra.vault

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class EntityCacheSpec : StringSpec({

    // NullEntityCache //////////////////////////////////////////////////////////////////////////

    "NullEntityCache.getOrPut always calls the provider" {
        var callCount = 0

        val result1 = NullEntityCache.getOrPut("id1") { callCount++; "value" }
        val result2 = NullEntityCache.getOrPut("id1") { callCount++; "value" }

        result1 shouldBe "value"
        result2 shouldBe "value"
        callCount shouldBe 2
    }

    "NullEntityCache.put returns the value" {
        val result = NullEntityCache.put("id1", "hello")

        result shouldBe "hello"
    }

    // DefaultEntityCache ///////////////////////////////////////////////////////////////////////

    "DefaultEntityCache.getOrPut caches on first call and returns cached on second" {
        val cache = DefaultEntityCache()
        var callCount = 0

        val result1 = cache.getOrPut("id1") { callCount++; "value" }
        val result2 = cache.getOrPut("id1") { callCount++; "other" }

        result1 shouldBe "value"
        result2 shouldBe "value"
        callCount shouldBe 1
    }

    "DefaultEntityCache.put stores value retrievable by getOrPut" {
        val cache = DefaultEntityCache()

        cache.put("id1", "stored")

        var providerCalled = false
        val result = cache.getOrPut("id1") { providerCalled = true; "from-provider" }

        result shouldBe "stored"
        providerCalled shouldBe false
    }

    "DefaultEntityCache.put returns the value" {
        val cache = DefaultEntityCache()

        val result = cache.put("id1", 42)

        result shouldBe 42
    }

    "DefaultEntityCache.clear empties the cache" {
        val cache = DefaultEntityCache()

        cache.put("id1", "value")
        cache.clear()

        var providerCalled = false
        val result = cache.getOrPut("id1") { providerCalled = true; "fresh" }

        result shouldBe "fresh"
        providerCalled shouldBe true
    }

    "DefaultEntityCache.getOrPut with different keys stores independently" {
        val cache = DefaultEntityCache()

        cache.getOrPut("id1") { "value1" }
        cache.getOrPut("id2") { "value2" }

        val result1 = cache.getOrPut("id1") { "other" }
        val result2 = cache.getOrPut("id2") { "other" }

        result1 shouldBe "value1"
        result2 shouldBe "value2"
    }
})
