package io.peekandpoke.ultra.cache

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * A refresh racing the application's own writes.
 *
 * Each loader call is held by a gate of its own, so the order of loads, writes and completions is
 * fixed by the test; the waits only give the loop time to run.
 */
class FastCacheRefreshRaceSpec : StringSpec() {

    /** Loader calls, each one held until its gate opens. */
    private class GatedLoader(size: Int) {
        val started = List(size) { CompletableDeferred<Unit>() }
        val gates = List(size) { CompletableDeferred<String>() }
        var calls = 0

        suspend fun load(): String {
            val call = calls++
            started[call].complete(Unit)

            return gates[call].await()
        }
    }

    init {
        "a refresh does not overwrite a value written while its loader ran" {
            val loader = GatedLoader(size = 10)
            val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
                refreshAfterWrite(100.milliseconds) { loader.load() }
            }

            cache.put("a", "old")
            loader.started[0].await()

            cache.put("a", "fresh")
            loader.gates[0].complete("refreshed from old")

            // longer than the loop needs to apply a refresh result, shorter than the next refresh
            delay(60.milliseconds)

            cache.get("a") shouldBe "fresh"
        }

        "keys claimed for refresh are loaded even when a hard-TTL eviction's listener throws" {
            // Driven by hand, as in FastCacheEvictionDeclineSpec, against a cache whose own loop only
            // drains: the listener is registered on the cache, the refresh behaviour is not.
            val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
                onEviction { _, _ -> error("eviction listener failed") }
            }
            val held = CompletableDeferred<String>()
            var loadsOfA = 0
            val behaviour = FastCache.RefreshAfterWriteBehaviour<String, String>(
                refreshAfter = 100.milliseconds,
                hardTtl = 400.milliseconds,
            ) { key ->
                if (key == "x") held.await() else { loadsOfA++; key }
            }

            cache.put("x", "1")
            cache.put("a", "1")
            delay(60.milliseconds)

            behaviour.process(cache, FastCache.ActionUpdates(listOf(FastCache.PutAction("x", "1"))))
            delay(150.milliseconds)

            // x is stale (its refresh hangs), a is written now
            behaviour.process(cache, FastCache.ActionUpdates(listOf(FastCache.PutAction("a", "1"))))
            delay(300.milliseconds)

            // One pass: x is past its hard TTL and its eviction's listener throws; a is stale and claimed
            runCatching { behaviour.process(cache, FastCache.ActionUpdates(emptyList())) }

            eventually(2.seconds) {
                loadsOfA shouldBe 1
            }
        }

        "a failed refresh does not release the claim of a newer refresh of the same key" {
            val loader = GatedLoader(size = 10)
            val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
                refreshAfterWrite(100.milliseconds) { loader.load() }
            }

            cache.put("a", "1")
            loader.started[0].await()

            // A write releases the first refresh's claim; once stale again, a second refresh claims the key
            cache.put("a", "2")
            eventually(2.seconds) { loader.calls shouldBe 2 }

            // The first refresh fails now. Releasing "its" claim must not release the second one's.
            loader.gates[0].completeExceptionally(IllegalStateException("first refresh failed"))

            // Several refresh periods while the second refresh is still running: no third may start
            delay(400.milliseconds)

            loader.calls shouldBe 2

            loader.gates[1].complete("done")
        }
    }
}
