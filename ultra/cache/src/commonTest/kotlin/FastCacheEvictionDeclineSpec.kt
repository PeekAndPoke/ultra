package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * What happens when `removeSilently` DECLINES an eviction (F3).
 *
 * `removeSilently` refuses to evict a key that was touched after the current action batch was
 * taken. That refusal is by design — the batch is stale by up to `loopDelay`. The defect was on the
 * caller side: every behaviour dropped the key from its own tracking BEFORE asking, so a declined
 * eviction left the entry in the map with nothing watching it.
 *
 * These tests drive a behaviour's `process()` by hand against a real cache that has NO registered
 * behaviours — its loop only drains pending actions every `loopDelay`. That makes the decline
 * deterministic: an un-drained `put`/`get` guarantees "declined", a drain-wait guarantees
 * "accepted". No thread races involved.
 */
class FastCacheEvictionDeclineSpec : StringSpec() {

    init {
        "expireAfterWrite: a declined eviction is retried once the pending read drains" {
            val cache = fastCache<String, String>(loopDelay = 100.milliseconds) {}
            val behaviour = FastCache.ExpireAfterWriteBehaviour<String, String>(ttl = 1.milliseconds)

            cache.put("a", "1")
            behaviour.process(cache, FastCache.ActionUpdates(listOf(FastCache.PutAction("a", "1"))))

            // the entry is now well past its ttl
            delay(50.milliseconds)

            // A read lands before the expiry runs. Its pending action makes removeSilently decline.
            cache.get("a")
            behaviour.process(cache, FastCache.ActionUpdates(emptyList()))

            // Sparing the just-read entry is the doorman's job and is correct
            cache.keys.contains("a") shouldBe true

            // Let the pending read drain, then run the expiry again. This behaviour deliberately
            // ignores reads, so nothing re-adds the key to its tracking - dropping it on the
            // declined attempt made the entry immortal.
            delay(300.milliseconds)
            behaviour.process(cache, FastCache.ActionUpdates(emptyList()))

            cache.keys.contains("a") shouldBe false
        }

        "refreshAfterWrite: a declined hard-TTL eviction is retried, not forgotten" {
            val cache = fastCache<String, String>(loopDelay = 100.milliseconds) {}
            val behaviour = FastCache.RefreshAfterWriteBehaviour<String, String>(
                refreshAfter = 5.milliseconds,
                hardTtl = 10.milliseconds,
            ) { error("a hard-expired entry must not be refreshed") }

            cache.put("a", "1")
            behaviour.process(cache, FastCache.ActionUpdates(listOf(FastCache.PutAction("a", "1"))))

            // well past the hard ttl - the hard branch wins over the refresh branch
            delay(50.milliseconds)

            cache.get("a")
            behaviour.process(cache, FastCache.ActionUpdates(emptyList()))

            cache.keys.contains("a") shouldBe true

            // Forgetting writeTimestamps on the declined attempt meant this entry would never be
            // refreshed and never hard-evicted again
            delay(300.milliseconds)
            behaviour.process(cache, FastCache.ActionUpdates(emptyList()))

            cache.keys.contains("a") shouldBe false
        }

        "maxMemoryUsage: a declined eviction does not corrupt totalSize" {
            val cache = fastCache<String, String>(loopDelay = 100.milliseconds) {}
            val behaviour = FastCache.MaxMemoryUsageBehaviour<String, String>(maxMemorySize = 1)

            cache.put("a", "1")

            // Mid-sleep, so the pending it creates cannot race the loop's first drain (the put
            // above can, which is why it is not used as the decline trigger)
            delay(150.milliseconds)
            cache.get("a")

            // handle() books the entry's size, then evictAllNecessary trips over the budget and
            // asks - and is declined because of the pending read
            behaviour.process(cache, FastCache.ActionUpdates(listOf(FastCache.PutAction("a", "1"))))

            // The entry survived (declined), so its bytes are still in the cache and must still be
            // counted. Decrementing before asking reported 0 for a cache that holds the entry.
            cache.keys.contains("a") shouldBe true
            behaviour.totalSize shouldBeGreaterThan 0L

            // After the pending action drains, the retry must evict and only THEN release the bytes
            delay(300.milliseconds)
            behaviour.process(cache, FastCache.ActionUpdates(emptyList()))

            cache.keys.contains("a") shouldBe false
            behaviour.totalSize shouldBe 0L
        }

        "maxEntries: process() returns when every candidate is declined, and retries next round" {
            val cache = fastCache<String, String>(loopDelay = 100.milliseconds) {}
            val behaviour = FastCache.MaxEntriesBehaviour<String, String>(maxEntries = 1)

            cache.put("a", "1")
            cache.put("b", "2")

            // Mid-sleep reads (see the maxMemoryUsage test) - both keys now have pending actions,
            // so both evictions will be declined. A size-based `while` loop over kept-tracked
            // entries would spin forever inside process() - a regression of that shows up as this
            // test (and the suite) hanging, not as an assertion.
            delay(150.milliseconds)
            cache.get("a")
            cache.get("b")

            behaviour.process(
                cache,
                FastCache.ActionUpdates(
                    listOf(
                        FastCache.PutAction("a", "1"),
                        FastCache.PutAction("b", "2"),
                    )
                )
            )

            // Over capacity for one round is the accepted trade-off of declining
            cache.size shouldBe 2

            // Once the pendings drain, the retry brings the cache back under its limit
            delay(300.milliseconds)
            behaviour.process(cache, FastCache.ActionUpdates(emptyList()))

            cache.size shouldBe 1
        }
    }
}
