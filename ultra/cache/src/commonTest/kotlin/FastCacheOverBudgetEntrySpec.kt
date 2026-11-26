package io.peekandpoke.ultra.cache

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * `maxMemoryUsage`: an entry whose own size exceeds the whole budget is evicted alone and never counted
 * into `totalSize`; the other entries are untouched (Caffeine's rule, ULTRA-00159 decision D1).
 *
 * Values are charged their own number, keys nothing, so every size is exact.
 */
class FastCacheOverBudgetEntrySpec : StringSpec() {

    /** Charges an Int value its own number and everything else (the keys) nothing; counts its calls. */
    private class OwnNumber : ObjectSizeEstimator {
        var calls = 0

        override fun estimate(obj: Any?): Long {
            calls++
            return (obj as? Int)?.toLong() ?: 0L
        }
    }

    /** A cache whose loop only drains, every 500 ms, and the tap that shows when it did. */
    private class Rig {
        val tap = ActionTap<String, Int>()
        val cache = fastCache<String, Int>(loopDelay = 500.milliseconds) { addBehaviour(tap) }

        /** Waits until the loop has drained [count] actions in all. */
        suspend fun drained(count: Int) {
            eventually(3.seconds) { (tap.actions.size >= count) shouldBe true }
        }
    }

    init {
        timeout = TEST_TIMEOUT_MS

        "an entry above the whole budget is evicted alone; the entries before it stay" {
            val cache = fastCache<String, Int>(loopDelay = 20.milliseconds) {
                maxMemoryUsage(100L, OwnNumber())
            }
            val behaviour = cache.behaviours.filterIsInstance<FastCache.MaxMemoryUsageBehaviour<String, Int>>().single()

            cache.put("a", 10)
            cache.put("b", 20)
            eventually(3.seconds) { behaviour.totalSize shouldBe 30L }

            cache.put("big", 500)

            eventually(3.seconds) { cache.keys shouldBe setOf("a", "b") }
            behaviour.totalSize shouldBe 30L

            keepReachable(cache)
        }

        "an entry overwritten with a value above the budget releases its old bytes and goes alone" {
            val rig = Rig()
            val behaviour = FastCache.MaxMemoryUsageBehaviour<String, Int>(100L, OwnNumber())

            rig.cache.put("a", 10)
            rig.cache.put("b", 20)
            rig.drained(2)
            behaviour.process(rig.cache, FastCache.ActionUpdates(rig.tap.takeNew()))
            behaviour.totalSize shouldBe 30L

            rig.cache.put("a", 500)
            rig.drained(3)
            behaviour.process(rig.cache, FastCache.ActionUpdates(rig.tap.takeNew()))

            rig.cache.keys shouldBe setOf("b")
            behaviour.totalSize shouldBe 20L
        }

        "an entry above the budget is spared while a write is pending, not counted; that write is sized next pass" {
            val rig = Rig()
            val estimator = OwnNumber()
            val behaviour = FastCache.MaxMemoryUsageBehaviour<String, Int>(100L, estimator)

            rig.cache.put("a", 10)
            rig.cache.put("big", 500)
            rig.drained(2)

            // a write lands before the behaviour runs: the eviction spares "big"
            rig.cache.put("big", 600)
            behaviour.process(rig.cache, FastCache.ActionUpdates(rig.tap.takeNew()))

            rig.cache.keys shouldBe setOf("a", "big")
            behaviour.totalSize shouldBe 10L
            // keys and values of the two puts
            estimator.calls shouldBe 4

            // the write arrives next pass: sized anew, still above the budget, evicted now
            rig.drained(3)
            behaviour.process(rig.cache, FastCache.ActionUpdates(rig.tap.takeNew()))

            rig.cache.keys shouldBe setOf("a")
            behaviour.totalSize shouldBe 10L
            estimator.calls shouldBe 6
        }

        "a pending read does not spare an entry above the budget" {
            val rig = Rig()
            val behaviour = FastCache.MaxMemoryUsageBehaviour<String, Int>(100L, OwnNumber())

            rig.cache.put("a", 10)
            rig.cache.put("big", 500)
            rig.drained(2)

            // a read lands before the behaviour runs: it keeps an entry in use, but this one never fits
            rig.cache.get("big")
            behaviour.process(rig.cache, FastCache.ActionUpdates(rig.tap.takeNew()))

            rig.cache.keys shouldBe setOf("a")
            behaviour.totalSize shouldBe 10L
        }

        "a spared entry above the budget, overwritten with a small value, is counted at its new size" {
            val rig = Rig()
            val behaviour = FastCache.MaxMemoryUsageBehaviour<String, Int>(100L, OwnNumber())

            rig.cache.put("a", 10)
            rig.cache.put("big", 500)
            rig.drained(2)

            rig.cache.put("big", 40)
            behaviour.process(rig.cache, FastCache.ActionUpdates(rig.tap.takeNew()))
            behaviour.totalSize shouldBe 10L

            rig.drained(3)
            behaviour.process(rig.cache, FastCache.ActionUpdates(rig.tap.takeNew()))

            rig.cache.keys shouldBe setOf("a", "big")
            behaviour.totalSize shouldBe 50L
        }

        "removing an entry above the budget that was never counted releases nothing" {
            val rig = Rig()
            val behaviour = FastCache.MaxMemoryUsageBehaviour<String, Int>(100L, OwnNumber())

            rig.cache.put("a", 10)
            rig.cache.put("big", 500)
            rig.drained(2)

            rig.cache.put("big", 600)
            behaviour.process(rig.cache, FastCache.ActionUpdates(rig.tap.takeNew()))
            behaviour.totalSize shouldBe 10L

            // the pending write, then the removal
            rig.cache.remove("big")
            rig.drained(4)
            behaviour.process(rig.cache, FastCache.ActionUpdates(rig.tap.takeNew()))

            rig.cache.keys shouldBe setOf("a")
            behaviour.totalSize shouldBe 10L

            // nothing of it is left behind: a key the behaviour does not track, seen by a read only,
            // is estimated from what was read, not charged an old size
            rig.cache.put("big", 5)
            rig.drained(5)
            rig.tap.takeNew()
            behaviour.process(rig.cache, FastCache.ActionUpdates(listOf(FastCache.ReadAction("big", 5))))

            rig.cache.keys shouldBe setOf("a", "big")
            behaviour.totalSize shouldBe 15L
        }

        "clear() with a spared entry above the budget leaves nothing counted" {
            val rig = Rig()
            val behaviour = FastCache.MaxMemoryUsageBehaviour<String, Int>(100L, OwnNumber())

            rig.cache.put("a", 10)
            rig.cache.put("big", 500)
            rig.drained(2)

            rig.cache.put("big", 600)
            behaviour.process(rig.cache, FastCache.ActionUpdates(rig.tap.takeNew()))
            behaviour.totalSize shouldBe 10L

            // the pending write, then a removal per entry
            rig.cache.clear()
            rig.drained(5)
            behaviour.process(rig.cache, FastCache.ActionUpdates(rig.tap.takeNew()))

            rig.cache.keys shouldBe emptySet()
            behaviour.totalSize shouldBe 0L

            // "big" again, small this time: counted like any entry
            rig.cache.put("big", 40)
            rig.drained(6)
            behaviour.process(rig.cache, FastCache.ActionUpdates(rig.tap.takeNew()))

            rig.cache.keys shouldBe setOf("big")
            behaviour.totalSize shouldBe 40L
        }

        "an entry of exactly the budget is kept and counted" {
            val rig = Rig()
            val behaviour = FastCache.MaxMemoryUsageBehaviour<String, Int>(100L, OwnNumber())

            rig.cache.put("a", 100)
            rig.drained(1)
            behaviour.process(rig.cache, FastCache.ActionUpdates(rig.tap.takeNew()))

            rig.cache.keys shouldBe setOf("a")
            behaviour.totalSize shouldBe 100L
        }

        "a refresh writing a value above the budget evicts that entry alone" {
            val cache = fastCache<String, Int>(loopDelay = 20.milliseconds) {
                maxMemoryUsage(100L, OwnNumber())
                // only "k" is refreshed, and grows past the budget: so it is the most recently used entry,
                // and evicting by access order would take "a" and "b" first. (A refresh of "a" and "b" made
                // them newer than "k" at times, and the old rule passed by evicting "k" first.)
                refreshAfterWrite(ttl = 50.milliseconds) { key ->
                    if (key == "k") 500 else error("only k is refreshed")
                }
            }
            val behaviour = cache.behaviours.filterIsInstance<FastCache.MaxMemoryUsageBehaviour<String, Int>>().single()

            cache.put("a", 10)
            cache.put("b", 20)
            cache.put("k", 30)

            eventually(3.seconds) { cache.keys shouldBe setOf("a", "b") }
            behaviour.totalSize shouldBe 30L

            keepReachable(cache)
        }
    }
}
