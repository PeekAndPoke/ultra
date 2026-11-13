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
