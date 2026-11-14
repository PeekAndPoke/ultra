package io.peekandpoke.ultra.cache

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
        timeout = TEST_TIMEOUT_MS

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

            // A throwing listener no longer starves the ones registered behind it
            eventually(3.seconds) {
                evicted shouldBe listOf("k0", "k1", "k2")
            }

            keepReachable(cache)
        }

        "the loop survives starting before the constructor has finished" {
            // Dispatchers.Unconfined runs the loop's first pass inside the constructor, before the rest
            // of the cache is initialized - what a pass on another thread can do at any time. A field
            // initialized after the loop's launch was still null there, and the loop died for good.
            val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
            val cache = fastCache<String, String>(scope = scope, loopDelay = 20.milliseconds) {
                maxEntries(1)
            }

            cache.put("a", "1")
            cache.put("b", "2")

            eventually(3.seconds) { cache.size shouldBe 1 }

            scope.cancel()
        }

        "a behaviour throwing a CancellationException does not end the loop" {
            // process() does not suspend, so this is user code failing, not the loop being cancelled
            val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
                addBehaviour(ThrowingBehaviour { CancellationException("thrown by a behaviour") })
                maxEntries(1)
            }

            cache.put("a", "1")
            cache.put("b", "2")

            eventually(3.seconds) { cache.size shouldBe 1 }

            // still alive later
            cache.put("c", "3")
            eventually(3.seconds) { cache.keys shouldBe setOf("c") }

            keepReachable(cache)
        }

        "a behaviour throwing a Throwable that is not an Exception does not end the loop" {
            val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
                addBehaviour(ThrowingBehaviour { NotAnException() })
                maxEntries(1)
            }

            cache.put("a", "1")
            cache.put("b", "2")

            eventually(3.seconds) { cache.size shouldBe 1 }

            cache.put("c", "3")
            eventually(3.seconds) { cache.keys shouldBe setOf("c") }

            keepReachable(cache)
        }

        "an eviction listener throwing a CancellationException does not end the loop" {
            val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
                maxEntries(1)
                onEviction { _, _ -> throw CancellationException("thrown by a listener") }
            }

            cache.put("a", "1")
            delay(60.milliseconds)
            cache.put("b", "2")
            delay(60.milliseconds)
            cache.put("c", "3")

            eventually(3.seconds) { cache.keys shouldBe setOf("c") }

            keepReachable(cache)
        }

        "a refresh loader throwing a Throwable that is not an Exception does not reach the scope's handler" {
            val handled = mutableListOf<Throwable>()
            val scope = CoroutineScope(SupervisorJob() + CoroutineExceptionHandler { _, e -> handled.add(e) })
            var loads = 0

            val cache = fastCache<String, String>(scope = scope, loopDelay = 20.milliseconds) {
                refreshAfterWrite(50.milliseconds) { loads++; throw NotAnException() }
            }

            cache.put("a", "1")

            // retried, i.e. the key was released each time
            eventually(3.seconds) { (loads >= 2) shouldBe true }

            handled shouldBe emptyList()

            scope.cancel()
            keepReachable(cache)
        }

        "an eviction listener that throws neither stops the other evictions of the pass nor the listeners behind it" {
            // Driven by hand: one process() call is one pass
            val called = mutableListOf<String>()
            val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
                onEviction { _, _ -> error("first listener fails") }
                onEviction { key, _ -> called.add(key) }
            }
            val behaviour = FastCache.ExpireAfterWriteBehaviour<String, String>(ttl = 50.milliseconds)
            val keys = List(100) { "k$it" }

            keys.forEach { cache.put(it, it) }
            delay(100.milliseconds)
            behaviour.process(cache, FastCache.ActionUpdates(keys.map { FastCache.PutAction(it, it) }))
            delay(100.milliseconds)

            runCatching { behaviour.process(cache, FastCache.ActionUpdates(emptyList())) }

            cache.size shouldBe 0
            called.size shouldBe 100
        }
    }
}

/** A behaviour that throws what [error] makes, in every pass. */
private class ThrowingBehaviour<K, V>(private val error: () -> Throwable) : FastCache.Behaviour<K, V> {
    override fun process(cache: FastCache<K, V>, updates: FastCache.ActionUpdates<K, V>) {
        throw error()
    }
}

/** Like `VaultException`: a Throwable that is not an Exception. */
private class NotAnException : Throwable("not an exception")

/** Config validation: a silently coerced limit is a misconfiguration that never surfaces. */
class FastCacheConfigValidationSpec : StringSpec() {

    init {
        timeout = TEST_TIMEOUT_MS

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
        }
    }
}

/** The builder must not keep a handle on the list it gave away. */
class FastCacheBuilderIsolationSpec : StringSpec() {

    init {
        timeout = TEST_TIMEOUT_MS

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
