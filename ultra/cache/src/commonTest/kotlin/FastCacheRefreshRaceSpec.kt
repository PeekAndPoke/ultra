package io.peekandpoke.ultra.cache

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.common.RunSync
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * A refresh racing the application's own writes.
 *
 * Each cache runs in a scope of its own, cancelled at the end, so a loader left hanging on its gate
 * does not outlive the test.
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
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            val cache = fastCache<String, String>(scope = scope, loopDelay = 20.milliseconds) {
                refreshAfterWrite(100.milliseconds) { loader.load() }
            }

            cache.put("a", "old")
            loader.started[0].await()

            cache.put("a", "fresh")
            loader.gates[0].complete("refreshed from old")

            // longer than the loop needs to apply a refresh result, shorter than the next refresh
            delay(60.milliseconds)

            cache.get("a") shouldBe "fresh"

            scope.cancel()
        }

        "a refresh does not start from a value written after the batch it decided on" {
            val tap = ActionTap<String, String>()
            val cache = fastCache<String, String>(loopDelay = 500.milliseconds) { addBehaviour(tap) }
            var loads = 0
            val refresh = FastCache.RefreshAfterWriteBehaviour<String, String>(10.milliseconds) { loads++; "loaded" }

            cache.put("a", "old")
            eventually(3.seconds) { (tap.actions.size >= 1) shouldBe true }
            refresh.process(cache, FastCache.ActionUpdates(listOf(FastCache.PutAction("a", "old"))))
            delay(50.milliseconds)

            // Written after the batch: the pass decided "a" is stale, but the fresh value is not.
            // Starting from it, the refresh would replace it with a load the write already made moot.
            cache.put("a", "fresh")
            refresh.process(cache, FastCache.ActionUpdates(emptyList()))
            delay(100.milliseconds)

            cache.get("a") shouldBe "fresh"
            loads shouldBe 0
        }

        "a throwable cutting process() short leaves no claimed key unlaunched for good" {
            val tap = ActionTap<FlakyKey, String>()
            val cache = fastCache<FlakyKey, String>(loopDelay = 500.milliseconds) { addBehaviour(tap) }
            // Both refreshes run at once on different threads: a plain list lost one of the adds
            val lock = Any()
            val loaded = mutableSetOf<String>()
            val refresh = FastCache.RefreshAfterWriteBehaviour<FlakyKey, String>(10.milliseconds) { key ->
                RunSync(lock) { loaded.add(key.id) }
                "loaded"
            }

            cache.put(FlakyKey("a"), "1")
            cache.put(FlakyKey("b"), "2")
            eventually(3.seconds) { (tap.actions.size >= 2) shouldBe true }

            // The behaviour holds equal twins of the cached keys, so launching a refresh looks the
            // twin up in the cache and calls its equals() - which throws once for "a"
            val twinA = FlakyKey("a")
            refresh.process(cache, FastCache.ActionUpdates(listOf(FastCache.PutAction(twinA, "1"), FastCache.PutAction(FlakyKey("b"), "2"))))
            delay(50.milliseconds)

            twinA.failNextEquals = true
            runCatching { refresh.process(cache, FastCache.ActionUpdates(emptyList())) }

            // Both were claimed, neither launched: the next pass must find them free again
            delay(50.milliseconds)
            refresh.process(cache, FastCache.ActionUpdates(emptyList()))

            eventually(3.seconds) { RunSync(lock) { loaded.toSet() } shouldBe setOf("a", "b") }
        }

        "a failed refresh does not release the claim of a newer refresh of the same key" {
            val loader = GatedLoader(size = 10)
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            val cache = fastCache<String, String>(scope = scope, loopDelay = 20.milliseconds) {
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

            scope.cancel()
            keepReachable(cache)
        }
    }
}

/** A key whose equals() throws once when told to. */
private class FlakyKey(val id: String) {
    var failNextEquals = false

    override fun equals(other: Any?): Boolean {
        if (failNextEquals) {
            failNextEquals = false
            error("equals failed once")
        }

        return other is FlakyKey && other.id == id
    }

    override fun hashCode(): Int = id.hashCode()
}
