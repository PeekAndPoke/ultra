package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ValueSortedMapAdditionalSpec : StringSpec() {
    init {
        "isEmpty returns true for new map" {
            val m = ValueSortedMap<String, Int, Int> { it }

            m.isEmpty() shouldBe true
            m.size shouldBe 0
        }

        "isEmpty returns false after put" {
            val m = ValueSortedMap<String, Int, Int> { it }

            m.put("a", 1)

            m.isEmpty() shouldBe false
        }

        "containsKey returns true for existing key" {
            val m = ValueSortedMap<String, Int, Int> { it }

            m.put("a", 1)

            m.containsKey("a") shouldBe true
        }

        "containsKey returns false for missing key" {
            val m = ValueSortedMap<String, Int, Int> { it }

            m.containsKey("missing") shouldBe false
        }

        "containsValue returns true for existing value" {
            val m = ValueSortedMap<String, Int, Int> { it }

            m.put("a", 42)

            m.containsValue(42) shouldBe true
        }

        "containsValue returns false for missing value" {
            val m = ValueSortedMap<String, Int, Int> { it }

            m.put("a", 1)

            m.containsValue(999) shouldBe false
        }

        "putAll inserts all entries" {
            val m = ValueSortedMap<String, Int, Int> { it }

            m.putAll(mapOf("a" to 10, "b" to 20, "c" to 5))

            m.size shouldBe 3
            m["a"] shouldBe 10
            m["b"] shouldBe 20
            m["c"] shouldBe 5
        }

        "putAll overwrites existing keys" {
            val m = ValueSortedMap<String, Int, Int> { it }

            m.put("a", 1)
            m.putAll(mapOf("a" to 99, "b" to 50))

            m.size shouldBe 2
            m["a"] shouldBe 99
            m["b"] shouldBe 50
        }

        "get returns null for missing key" {
            val m = ValueSortedMap<String, Int, Int> { it }

            m["nonexistent"] shouldBe null
        }

        "size tracks inserts and removes correctly" {
            val m = ValueSortedMap<String, Int, Int> { it }

            m.put("a", 1)
            m.put("b", 2)
            m.size shouldBe 2

            m.remove("a")
            m.size shouldBe 1

            m.remove("a") // already removed
            m.size shouldBe 1

            m.remove("b")
            m.size shouldBe 0
        }

        "replacing same key does not increase size" {
            val m = ValueSortedMap<String, Int, Int> { it }

            m.put("a", 1)
            m.put("a", 2)
            m.put("a", 3)

            m.size shouldBe 1
            m["a"] shouldBe 3
        }
    }
}
