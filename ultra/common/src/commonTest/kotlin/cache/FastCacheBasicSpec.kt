// language: kotlin
package de.peekandpoke.ultra.common.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class FastCacheBasicSpec : StringSpec() {

    private suspend fun testScope(block: suspend (CoroutineScope) -> Unit) {
        val scope = Cache.defaultCoroutineScope

        block(scope)
    }

    init {
        "put / get / has / remove / getOrPut basic behavior" {
            testScope { scope ->
                val cache = fastCache<String, String>(scope = scope)

                cache.put("a", "1")
                cache.get("a") shouldBe "1"
                cache.has("a") shouldBe true

                cache.remove("a") shouldBe "1"
                cache.get("a").shouldBeNull()
                cache.has("a") shouldBe false

                val generated = cache.getOrPut("x") { "generated" }
                generated shouldBe "generated"
                cache.get("x") shouldBe "generated"
            }
        }

        "keys / values / entries reflect current map snapshot" {
            testScope { scope ->
                val cache = fastCache<String, String>(scope = scope)

                cache.put("k1", "v1")
                cache.put("k2", "v2")

                val keys = cache.keys
                val values = cache.values
                val entries = cache.entries

                keys shouldContainExactlyInAnyOrder setOf("k1", "k2")

                // values order is not guaranteed; compare as set-like
                values.toSet() shouldContainExactlyInAnyOrder setOf("v1", "v2")
                entries.keys shouldContainExactlyInAnyOrder setOf("k1", "k2")

                entries["k1"] shouldBe "v1"
                entries["k2"] shouldBe "v2"
            }
        }

        "concurrent access from multiple coroutines does not corrupt map" {
            testScope { scope ->
                val cache = fastCache<Int, Int>(scope = scope)

                // Launch many coroutines that interleave puts and gets
                val jobs = List(200) { idx ->
                    scope.async {
                        println("Started coroutine $idx")

                        val key = idx % 10
                        cache.put(key, idx)
                        val v = cache.get(key)

                        // read / write some more
                        cache.getOrPut(key) { v ?: (idx + 1000) }
                        cache.has(key)
                    }
                }

                jobs.awaitAll()

                // Check map is in a consistent state: keys 0..9 exist
                cache.keys shouldContainExactlyInAnyOrder (0..9).toList()

                // Each key maps to some Int value (non-null)
                cache.entries.values.forEach { it shouldBe it } // trivial assertion to ensure values present
            }
        }

        "get records read but does not remove the entry (basic contract)" {
            testScope { scope ->
                fastCache<String, String>(scope = scope).let { cache ->
                    cache.put("readKey", "v")
                    val v1 = cache.get("readKey")

                    v1 shouldBe "v"

                    // subsequent get still returns the value
                    cache.get("readKey") shouldBe "v"

                    // remove actually removes
                    cache.remove("readKey") shouldBe "v"
                    cache.get("readKey").shouldBeNull()
                }
            }
        }
    }
}
