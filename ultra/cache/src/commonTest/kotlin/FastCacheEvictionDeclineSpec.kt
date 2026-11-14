package io.peekandpoke.ultra.cache

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * When `removeSilently` spares an entry from an eviction, and what the behaviours do then.
 *
 * `removeSilently` spares a key that was touched after the batch the behaviour decided on was taken:
 * for the access-ordered behaviours (expireAfterAccess, maxEntries, maxMemoryUsage) any pending
 * action, for write-based expiry (expireAfterWrite, the refresh hard TTL) only a pending put (M1).
 * A spared key stays tracked and is retried next pass (F3: dropping it left the entry unwatched). A
 * key that is already gone is not "spared": the behaviour drops it and does not evict a live entry
 * in its place.
 *
 * These tests drive a behaviour's `process()` by hand against a cache whose own loop only drains, every
 * 500 ms; an [ActionTap] shows when it did. Right after a drain the next one is about 500 ms away, so
 * an action made then is pending for sure.
 */
class FastCacheEvictionDeclineSpec : StringSpec() {

    /** A cache whose loop only drains, and the tap that shows when it did. */
    private class Rig {
        val tap = ActionTap<String, String>()
        val cache = fastCache<String, String>(loopDelay = 500.milliseconds) { addBehaviour(tap) }

        /** Waits until the loop has drained [count] actions in all. */
        suspend fun drained(count: Int) {
            eventually(3.seconds) { (tap.actions.size >= count) shouldBe true }
        }
    }

    init {
        timeout = TEST_TIMEOUT_MS

        "expireAfterWrite: a pending read does not spare an expired entry" {
            val rig = Rig()
            val behaviour = FastCache.ExpireAfterWriteBehaviour<String, String>(ttl = 1.milliseconds)

            rig.cache.put("a", "1")
            rig.drained(1)
            behaviour.process(rig.cache, FastCache.ActionUpdates(listOf(FastCache.PutAction("a", "1"))))

            // the entry is now well past its ttl
            delay(50.milliseconds)

            // A read lands before the expiry runs. Reads do not reset a write TTL, so it spares
            // nothing: sparing it let a key read in every pass outlive its TTL for good (M1).
            rig.cache.get("a")
            behaviour.process(rig.cache, FastCache.ActionUpdates(emptyList()))

            rig.cache.keys.contains("a") shouldBe false
        }

        "expireAfterWrite: a pending put spares the entry it just wrote, also when read after" {
            val rig = Rig()
            val behaviour = FastCache.ExpireAfterWriteBehaviour<String, String>(ttl = 1.milliseconds)

            rig.cache.put("a", "1")
            rig.drained(1)
            behaviour.process(rig.cache, FastCache.ActionUpdates(listOf(FastCache.PutAction("a", "1"))))
            delay(50.milliseconds)

            // A write lands before the expiry runs: the expiry acts on the old write time, the new
            // value must survive; its put re-stamps the entry next pass. The read after it must not
            // clear the "put pending" mark.
            rig.cache.put("a", "2")
            rig.cache.get("a")
            behaviour.process(rig.cache, FastCache.ActionUpdates(emptyList()))

            rig.cache.entries shouldBe mapOf("a" to "2")
        }

        "expireAfterAccess: a pending read spares the entry" {
            val rig = Rig()
            val behaviour = FastCache.ExpireAfterAccessBehaviour<String, String>(ttl = 1.milliseconds)

            rig.cache.put("a", "1")
            rig.drained(1)
            behaviour.process(rig.cache, FastCache.ActionUpdates(listOf(FastCache.PutAction("a", "1"))))
            delay(50.milliseconds)

            // An access lands before the expiry runs: for this behaviour that is what keeps it alive
            rig.cache.get("a")
            behaviour.process(rig.cache, FastCache.ActionUpdates(emptyList()))

            rig.cache.keys.contains("a") shouldBe true
        }

        "refreshAfterWrite: a pending read does not spare a hard-expired entry" {
            val rig = Rig()
            val behaviour = FastCache.RefreshAfterWriteBehaviour<String, String>(
                refreshAfter = 5.milliseconds,
                hardTtl = 10.milliseconds,
            ) { error("a hard-expired entry must not be refreshed") }

            rig.cache.put("a", "1")
            rig.drained(1)
            behaviour.process(rig.cache, FastCache.ActionUpdates(listOf(FastCache.PutAction("a", "1"))))

            // well past the hard ttl - the hard branch wins over the refresh branch
            delay(50.milliseconds)

            rig.cache.get("a")
            behaviour.process(rig.cache, FastCache.ActionUpdates(emptyList()))

            rig.cache.keys.contains("a") shouldBe false
        }

        "refreshAfterWrite: a pending put spares the entry from the hard TTL, also when read after" {
            val rig = Rig()
            val behaviour = FastCache.RefreshAfterWriteBehaviour<String, String>(
                refreshAfter = 5.milliseconds,
                hardTtl = 10.milliseconds,
            ) { error("a hard-expired entry must not be refreshed") }

            rig.cache.put("a", "1")
            rig.drained(1)
            behaviour.process(rig.cache, FastCache.ActionUpdates(listOf(FastCache.PutAction("a", "1"))))
            delay(50.milliseconds)

            rig.cache.put("a", "2")
            rig.cache.get("a")
            behaviour.process(rig.cache, FastCache.ActionUpdates(emptyList()))

            rig.cache.entries shouldBe mapOf("a" to "2")
        }

        "maxEntries: an entry removed earlier in the same pass is not replaced by evicting a live one" {
            val rig = Rig()
            val expiry = FastCache.ExpireAfterWriteBehaviour<String, String>(ttl = 1.milliseconds)
            val bound = FastCache.MaxEntriesBehaviour<String, String>(maxEntries = 2)

            listOf("A", "B", "C").forEach { rig.cache.put(it, it) }
            rig.drained(3)

            val first = FastCache.ActionUpdates(listOf(FastCache.PutAction("A", "A"), FastCache.PutAction("B", "B")))
            expiry.process(rig.cache, first)
            bound.process(rig.cache, first)
            delay(50.milliseconds)

            // One pass: the expiry removes A and B (their removals are pending now); the bound has not
            // seen those removals and counts A, B and C. A and B are gone, not spared: dropping them
            // meets the bound, and C stays.
            val second = FastCache.ActionUpdates(listOf(FastCache.PutAction("C", "C")))
            expiry.process(rig.cache, second)
            bound.process(rig.cache, second)

            rig.cache.keys shouldBe setOf("C")
        }

        "maxEntries: an entry gone earlier in the pass, but not first in LRU order, is not replaced by a live one" {
            val rig = Rig()
            val expiry = FastCache.ExpireAfterWriteBehaviour<String, String>(ttl = 1.milliseconds)
            val bound = FastCache.MaxEntriesBehaviour<String, String>(maxEntries = 2)

            listOf("A", "B", "C").forEach { rig.cache.put(it, it) }
            rig.drained(3)

            val first = FastCache.ActionUpdates(listOf(FastCache.PutAction("A", "A")))
            expiry.process(rig.cache, first)
            bound.process(rig.cache, first)
            delay(50.milliseconds)

            // One pass: the expiry removes A. The bound saw A read after B was written, so its LRU
            // order is B, A, C - the gone entry is not first. The cache really holds B and C.
            expiry.process(rig.cache, FastCache.ActionUpdates(listOf(FastCache.PutAction("B", "B"), FastCache.PutAction("C", "C"))))
            bound.process(
                rig.cache,
                FastCache.ActionUpdates(
                    listOf(FastCache.PutAction("B", "B"), FastCache.ReadAction("A", "A"), FastCache.PutAction("C", "C"))
                ),
            )

            rig.cache.keys shouldBe setOf("B", "C")
        }

        "maxMemoryUsage: an entry gone earlier in the pass, but not first in LRU order, is not replaced by a live one" {
            val rig = Rig()
            val expiry = FastCache.ExpireAfterWriteBehaviour<String, String>(ttl = 1.milliseconds)
            // every value costs 1, keys nothing: a budget of 2 entries
            val bound = FastCache.MaxMemoryUsageBehaviour<String, String>(
                maxMemorySize = 2,
                estimator = object : ObjectSizeEstimator {
                    override fun estimate(obj: Any?): Long = if (obj is String && obj.length == 1 && obj[0].isLowerCase()) 1L else 0L
                },
            )

            listOf("A", "B", "C").forEach { rig.cache.put(it, it.lowercase()) }
            rig.drained(3)

            val first = FastCache.ActionUpdates(listOf(FastCache.PutAction("A", "a")))
            expiry.process(rig.cache, first)
            bound.process(rig.cache, first)
            delay(50.milliseconds)

            expiry.process(rig.cache, FastCache.ActionUpdates(listOf(FastCache.PutAction("B", "b"), FastCache.PutAction("C", "c"))))
            bound.process(
                rig.cache,
                FastCache.ActionUpdates(
                    listOf(FastCache.PutAction("B", "b"), FastCache.ReadAction("A", "a"), FastCache.PutAction("C", "c"))
                ),
            )

            rig.cache.keys shouldBe setOf("B", "C")
            bound.totalSize shouldBe 2L
        }

        "maxMemoryUsage: a declined eviction does not corrupt totalSize" {
            val rig = Rig()
            val behaviour = FastCache.MaxMemoryUsageBehaviour<String, String>(maxMemorySize = 1)

            rig.cache.put("a", "1")
            rig.drained(1)
            rig.cache.get("a")

            // handle() books the entry's size, then evictAllNecessary trips over the budget and
            // asks - and is declined because of the pending read
            behaviour.process(rig.cache, FastCache.ActionUpdates(listOf(FastCache.PutAction("a", "1"))))

            // The entry survived (declined), so its bytes are still in the cache and must still be
            // counted. Decrementing before asking reported 0 for a cache that holds the entry.
            rig.cache.keys.contains("a") shouldBe true
            behaviour.totalSize shouldBeGreaterThan 0L

            // After the pending read drains, the retry must evict and only THEN release the bytes
            rig.drained(2)
            behaviour.process(rig.cache, FastCache.ActionUpdates(emptyList()))

            rig.cache.keys.contains("a") shouldBe false
            behaviour.totalSize shouldBe 0L
        }

        "maxEntries: process() returns when every candidate is declined, and retries next pass" {
            val rig = Rig()
            val behaviour = FastCache.MaxEntriesBehaviour<String, String>(maxEntries = 1)

            rig.cache.put("a", "1")
            rig.cache.put("b", "2")
            rig.drained(2)

            // Both keys now have pending reads, so both evictions are declined. A size-based `while`
            // loop over kept-tracked entries would spin forever inside process() - a regression of
            // that shows up as this test (and the suite) hanging, not as an assertion.
            rig.cache.get("a")
            rig.cache.get("b")

            behaviour.process(
                rig.cache,
                FastCache.ActionUpdates(listOf(FastCache.PutAction("a", "1"), FastCache.PutAction("b", "2")))
            )

            // A soft bound: over it while the reads are pending (and for keys read in every pass, for
            // as long as that lasts - a soft bound by decision, FastCacheSoftBoundSpec)
            rig.cache.size shouldBe 2

            // Once the reads drain, the retry brings the cache back under its bound
            rig.drained(4)
            behaviour.process(rig.cache, FastCache.ActionUpdates(emptyList()))

            rig.cache.size shouldBe 1
        }
    }
}
