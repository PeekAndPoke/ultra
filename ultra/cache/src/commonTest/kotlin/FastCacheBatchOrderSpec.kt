package io.peekandpoke.ultra.cache

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Several actions on one key inside one loop window.
 *
 * A batch holds every action since the last loop pass. A read after a put, or a miss after a remove,
 * must not hide the put or the remove from the behaviours that act on it.
 */
class FastCacheBatchOrderSpec : StringSpec() {

    init {
        "expireAfterWrite: a read right after the put does not hide the write" {
            val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
                expireAfterWrite(100.milliseconds)
            }

            cache.put("a", "1")
            cache.get("a")

            eventually(2.seconds) {
                cache.keys.shouldBeEmpty()
            }
        }

        "refreshAfterWrite: a key read constantly is still refreshed again and again" {
            var loads = 0

            val cache = fastCache<String, String> {
                refreshAfterWrite(100.milliseconds) { loads++; it }
            }

            cache.put("a", "1")

            coroutineScope {
                // a read lands in the same batch as each refresh's put
                val reader = launch {
                    while (isActive) {
                        cache.get("a")
                        delay(1.milliseconds)
                    }
                }

                eventually(3.seconds) {
                    loads shouldBeGreaterThanOrEqual 3
                }

                reader.cancel()
            }
        }

        "maxEntries: a miss right after clear() does not keep the cleared key tracked" {
            val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
                maxEntries(2)
            }

            cache.put("a", "1")
            cache.put("b", "2")
            delay(100.milliseconds)

            cache.clear()
            cache.get("a")
            delay(100.milliseconds)

            cache.put("c", "3")
            cache.put("d", "4")

            // Several loop passes. A ghost "a" is never evictable, so it takes a slot and pushes out a
            // live entry instead. Checked after the loop has run: right after a put the map holds
            // more entries than the limit for a moment, whatever the behaviour does.
            delay(200.milliseconds)

            cache.keys shouldBe setOf("c", "d")
        }

        "refreshAfterWrite: a miss right after remove() stops the key being reloaded" {
            var loads = 0
            val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
                refreshAfterWrite(100.milliseconds) { loads++; it }
            }

            cache.put("a", "1")
            delay(50.milliseconds)

            cache.remove("a")
            cache.get("a")

            // well past the refresh TTL: a ghost write time would have launched the loader by now
            delay(300.milliseconds)

            loads shouldBe 0
        }

        "maxMemoryUsage: a miss right after remove() gives the bytes back" {
            val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
                maxMemoryUsage(1_000_000)
            }
            val behaviour = cache.behaviours
                .filterIsInstance<FastCache.MaxMemoryUsageBehaviour<String, String>>().single()

            cache.put("a", "1")
            eventually(2.seconds) {
                (behaviour.totalSize > 0L) shouldBe true
            }

            cache.remove("a")
            cache.get("a")

            eventually(2.seconds) {
                behaviour.totalSize shouldBe 0L
            }
        }
    }
}
