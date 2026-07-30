package io.peekandpoke.ultra.cache

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

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
