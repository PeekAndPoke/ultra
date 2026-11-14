package io.peekandpoke.ultra.cache

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * The processing loop must outlive a failing behaviour.
 *
 * Everything the cache does beyond `get`/`put` — expiry, eviction, the memory bound, statistics,
 * refresh — happens in one coroutine. An unguarded throw ended it permanently while the cache went
 * on accepting writes, so the failure looked like healthy unbounded growth.
 */
class FastCacheLoopResilienceSpec : StringSpec() {

    init {
        "a throwing eviction handler does not kill the processing loop" {
            val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
                maxEntries(1)
                onEviction { _, _ -> error("eviction handler blew up") }
            }

            cache.put("a", "1")
            cache.put("b", "2")

            delay(300.milliseconds)

            // the first eviction ran (removeSilently removes before it notifies)
            cache.size shouldBe 1

            // ...and the loop is still alive, which is the part that regressed
            cache.put("c", "3")

            delay(300.milliseconds)

            cache.size shouldBe 1
        }

        "a behaviour that throws on every iteration still lets the others run" {
            val evicted = mutableListOf<String>()

            val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
                maxEntries(1)
                // registered first, so it throws before the statistics listener would be reached
                onEviction { _, _ -> error("eviction handler blew up") }
                onEviction { key, _ -> evicted.add(key) }
                statistics()
            }

            repeat(4) { i ->
                cache.put("k$i", "v$i")
                delay(60.milliseconds)
            }

            delay(200.milliseconds)

            // The cap is still enforced after repeated failures
            cache.size shouldBe 1

            // A throwing listener still starves the ones registered behind it - that is a separate
            // defect from the loop dying, and it is deliberately pinned here rather than fixed
            evicted.size shouldBe 0
        }
    }
}

/** Config validation: a silently coerced limit is a misconfiguration that never surfaces. */
class FastCacheConfigValidationSpec : StringSpec() {

    init {
        "maxEntries rejects a non-positive limit instead of coercing it to 1" {
            // Previously maxEntries(0) - written meaning "disable" - silently became a 1-entry cache
            shouldThrow<IllegalArgumentException> {
                fastCache<String, String> { maxEntries(0) }
            }

            shouldThrow<IllegalArgumentException> {
                fastCache<String, String> { maxEntries(-1) }
            }
        }

        "maxMemoryUsage rejects a non-positive budget" {
            // A zero budget made every loop iteration evict the entire cache
            shouldThrow<IllegalArgumentException> {
                fastCache<String, String> { maxMemoryUsage(0) }
            }
        }

        "a zero or negative expiry TTL does not throw: the entry expires at the next pass" {
            listOf(Duration.ZERO, (-1).milliseconds).forEach { ttl ->
                val caches = listOf(
                    fastCache<String, String>(loopDelay = 20.milliseconds) { expireAfterAccess(ttl) },
                    fastCache<String, String>(loopDelay = 20.milliseconds) { expireAfterWrite(ttl) },
                )

                caches.forEach { cache ->
                    cache.put("a", "1")

                    eventually(3.seconds) {
                        cache.keys shouldBe emptySet()
                    }
                }
            }
        }

        "an INFINITE expiry TTL never expires" {
            val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
                expireAfterAccess(Duration.INFINITE)
                expireAfterWrite(Duration.INFINITE)
            }

            cache.put("a", "1")
            delay(200.milliseconds)

            cache.keys shouldBe setOf("a")
        }

        "a zero or negative refresh TTL does not throw: no refresh, the entry expires at the next pass" {
            // Not the old reading of 0, "reload every key on every pass", which hammers the source
            listOf(Duration.ZERO, (-1).milliseconds).forEach { ttl ->
                var loads = 0
                val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
                    refreshAfterWrite(ttl, hardTtl = Duration.INFINITE) { loads++; it }
                }

                cache.put("a", "1")

                eventually(3.seconds) {
                    cache.keys shouldBe emptySet()
                }

                loads shouldBe 0
            }
        }

        "a zero or negative hard TTL does not throw: the entry expires at the next pass" {
            var loads = 0
            val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
                refreshAfterWrite(1.seconds, hardTtl = Duration.ZERO) { loads++; it }
            }

            cache.put("a", "1")

            eventually(3.seconds) {
                cache.keys shouldBe emptySet()
            }

            loads shouldBe 0
        }

        "a hard TTL not above the refresh TTL does not throw: the entry expires at it, unrefreshed" {
            var loads = 0
            val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
                refreshAfterWrite(200.milliseconds, hardTtl = 100.milliseconds) { loads++; it }
            }

            cache.put("a", "1")

            eventually(3.seconds) {
                cache.keys shouldBe emptySet()
            }

            // well past the refresh TTL: the hard branch is checked first, so it never refreshed
            delay(200.milliseconds)

            loads shouldBe 0

            keepReachable(cache)
        }

        "a sub-millisecond expiry TTL counts as 1 ms instead of truncating to 0" {
            // Driven by hand against a drained cache, as in FastCacheEvictionDeclineSpec: process()
            // stamps the write with `now` and checks expiry with the same `now`, so only a TTL of 0
            // evicts in that very call.
            val behaviours = listOf(
                FastCache.ExpireAfterAccessBehaviour<String, String>(ttl = 500.microseconds),
                FastCache.ExpireAfterWriteBehaviour(ttl = 500.microseconds),
            )

            behaviours.forEach { behaviour ->
                val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {}
                cache.put("a", "1")
                delay(100.milliseconds)

                behaviour.process(cache, FastCache.ActionUpdates(listOf(FastCache.PutAction("a", "1"))))

                cache.keys shouldBe setOf("a")
            }
        }

        "a sub-millisecond refresh TTL counts as 1 ms instead of truncating to 0" {
            var loads = 0
            val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {}
            val behaviour = FastCache.RefreshAfterWriteBehaviour<String, String>(refreshAfter = 500.microseconds) {
                loads++
                it
            }

            cache.put("a", "1")
            delay(100.milliseconds)

            // With a TTL of 0 this very call finds the entry stale and launches the loader
            behaviour.process(cache, FastCache.ActionUpdates(listOf(FastCache.PutAction("a", "1"))))
            delay(100.milliseconds)

            loads shouldBe 0

            keepReachable(cache)
        }
    }
}

/** The builder must not keep a handle on the list it gave away. */
class FastCacheBuilderIsolationSpec : StringSpec() {

    init {
        "adding a behaviour after build() does not affect the built cache" {
            val builder = FastCache.Builder<String, String>(loopDelay = 20.milliseconds)
            builder.maxEntries(10)

            val cache = builder.build()

            cache.behaviours.size shouldBe 1

            // Handing the builder's own mutable list to the cache would let this reach into an
            // already-running cache - and the added behaviour's eviction listener would never be
            // registered, because that happens once, in init.
            builder.maxEntries(1)

            cache.behaviours.size shouldBe 1
        }
    }
}
