package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class NullableCacheSpec : StringSpec({

    "get returns null for an unknown key" {
        NullableCache<String, String>().get("nope") shouldBe null
    }

    "has distinguishes an absent key from one cached as null" {
        val cache = NullableCache<String, String>()

        cache.put("known", null)

        cache.has("known") shouldBe true
        cache.has("absent") shouldBe false
        cache.get("known") shouldBe null
    }

    "put stores and returns the value" {
        val cache = NullableCache<String, String>()

        cache.put("a", "1") shouldBe "1"
        cache.get("a") shouldBe "1"
    }

    "put overwrites a previously cached null" {
        val cache = NullableCache<String, String>()

        cache.put("a", null)
        cache.put("a", "1")

        cache.get("a") shouldBe "1"
    }

    "size counts keys cached as null" {
        val cache = NullableCache<String, String>()

        cache.put("a", "1")
        cache.put("b", null)

        cache.size shouldBe 2
    }

    "clear drops everything" {
        val cache = NullableCache<String, String>()

        cache.put("a", "1")
        cache.put("b", null)
        cache.clear()

        cache.size shouldBe 0
        cache.has("b") shouldBe false
    }

    // getOrPut ////////////////////////////////////////////////////////////////////////////////////

    "getOrPut runs the provider once for a hit" {
        val cache = NullableCache<String, String>()
        var calls = 0

        cache.getOrPut("a") { calls++; "1" } shouldBe "1"
        cache.getOrPut("a") { calls++; "1" } shouldBe "1"

        calls shouldBe 1
    }

    "getOrPut caches a negative result — the whole point" {
        val cache = NullableCache<String, String>()
        var calls = 0

        cache.getOrPut("missing") { calls++; null } shouldBe null
        cache.getOrPut("missing") { calls++; null } shouldBe null
        cache.getOrPut("missing") { calls++; null } shouldBe null

        calls shouldBe 1
        cache.has("missing") shouldBe true
    }

    // getOrPutAsync ///////////////////////////////////////////////////////////////////////////////

    "getOrPutAsync runs the provider once for a hit" {
        val cache = NullableCache<String, String>()
        var calls = 0

        cache.getOrPutAsync("a") { calls++; "1" } shouldBe "1"
        cache.getOrPutAsync("a") { calls++; "1" } shouldBe "1"

        calls shouldBe 1
    }

    "getOrPutAsync caches a negative result" {
        val cache = NullableCache<String, String>()
        var calls = 0

        cache.getOrPutAsync("missing") { calls++; null } shouldBe null
        cache.getOrPutAsync("missing") { calls++; null } shouldBe null

        calls shouldBe 1
    }

    "getOrPutAsync sees what getOrPut cached" {
        val cache = NullableCache<String, String>()
        var calls = 0

        cache.getOrPut("a") { calls++; "1" }
        cache.getOrPutAsync("a") { calls++; "2" } shouldBe "1"

        calls shouldBe 1
    }

    "a provider that throws caches nothing" {
        val cache = NullableCache<String, String>()

        runCatching { cache.getOrPut("a") { error("boom") } }

        cache.has("a") shouldBe false
    }
})
