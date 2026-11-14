package io.peekandpoke.ultra.cache

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class FastCacheClearSpec : StringSpec() {
    init {
        timeout = TEST_TIMEOUT_MS

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

        "behaviours forget cleared entries: maxEntries does not count them against new ones" {
            val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
                maxEntries(2)
            }

            cache.put("a", "1")
            cache.put("b", "2")
            delay(100.milliseconds)

            cache.clear()
            cache.put("c", "3")
            cache.put("d", "4")
            delay(200.milliseconds)

            // Still tracking a and b, the behaviour saw four entries, could not evict the two that
            // are gone, and evicted the two real ones instead
            cache.keys shouldBe setOf("c", "d")
        }

        "behaviours forget cleared entries: maxMemoryUsage releases their bytes" {
            val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
                maxMemoryUsage(1_000_000)
            }
            val behaviour = cache.behaviours
                .filterIsInstance<FastCache.MaxMemoryUsageBehaviour<String, String>>().single()

            cache.put("a", "1")
            eventually(3.seconds) {
                (behaviour.totalSize > 0L) shouldBe true
            }

            cache.clear()

            eventually(3.seconds) {
                behaviour.totalSize shouldBe 0L
            }

            keepReachable(cache)
        }

        "behaviours forget cleared entries: refreshAfterWrite drops their write times" {
            // Driven by hand with what the cache recorded - see FastCacheBatchOrderSpec
            val tap = ActionTap<String, String>()
            val cache = fastCache<String, String>(loopDelay = 20.milliseconds) { addBehaviour(tap) }
            var loads = 0
            val refresh = FastCache.RefreshAfterWriteBehaviour<String, String>(100.milliseconds) { loads++; it }

            cache.put("a", "1")
            eventually(2.seconds) {
                tap.actions.lastOrNull().shouldBeInstanceOf<FastCache.PutAction<String, String>>()
            }
            refresh.process(cache, FastCache.ActionUpdates(tap.takeNew()))

            cache.clear()
            eventually(2.seconds) {
                tap.actions.lastOrNull().shouldBeInstanceOf<FastCache.RemoveAction<String, String>>()
            }
            refresh.process(cache, FastCache.ActionUpdates(tap.takeNew()))

            refreshStillTracksA(cache, refresh) { loads } shouldBe false
        }

        "clear() is not an eviction: onEviction and evictionCount do not fire" {
            val evicted = mutableListOf<String>()
            val stats = FastCache.StatisticsBehaviour<String, String>()
            val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
                onEviction { key, _ -> evicted.add(key) }
                addBehaviour(stats)
            }

            cache.put("a", "1")
            cache.put("b", "2")
            delay(100.milliseconds)

            cache.clear()

            // several loop passes, so the RemoveActions of the clear have been processed
            delay(200.milliseconds)

            evicted shouldBe emptyList()
            stats.snapshot().evictionCount shouldBe 0L

            keepReachable(cache)
        }

        "actions still pending at clear are counted by the statistics" {
            val stats = FastCache.StatisticsBehaviour<String, String>()
            val cache = fastCache<String, String>(loopDelay = 300.milliseconds) {
                addBehaviour(stats)
            }

            // past the loop's first drain, well before its second
            delay(50.milliseconds)

            cache.get("x")
            cache.put("a", "1")
            cache.get("a")
            cache.clear()

            delay(500.milliseconds)

            stats.snapshot() shouldBe FastCache.StatisticsBehaviour.CacheStats(
                hitCount = 1,
                missCount = 1,
                putCount = 1,
                evictionCount = 0,
            )

            keepReachable(cache)
        }
    }
}
