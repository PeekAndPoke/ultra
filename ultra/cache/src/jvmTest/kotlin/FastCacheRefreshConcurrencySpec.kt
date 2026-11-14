package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.concurrent.atomic.AtomicIntegerArray
import kotlin.time.Duration.Companion.milliseconds

/**
 * The refresh bookkeeping of [FastCache.RefreshAfterWriteBehaviour] under real threads (F4).
 *
 * A refresh that fails releases its key from the "refreshing" set on the thread the loader ran on,
 * while `process()` adds keys to that set on the loop thread. With an unsynchronised set, a release
 * lost to a concurrent resize leaves the key marked as refreshing forever: never refreshed again.
 */
class FastCacheRefreshConcurrencySpec : StringSpec() {

    /** Checks [condition] every 20 ms for up to 10 s; returns whether it became true. */
    private fun pollUntil(condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + 10_000

        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(20)
        }

        return condition()
    }

    init {
        timeout = TEST_TIMEOUT_MS

        "a key released by a failed refresh on another thread is refreshed again" {
            val keys = 20_000

            // The release is lost mostly while the set grows, so each trial starts with a fresh one.
            // One trial misses the race about one run in five; five trials make a miss very unlikely.
            repeat(5) {
                val loadsPerKey = AtomicIntegerArray(keys)

                // A scope per trial, cancelled at its end: each pass launches a loader for every
                // released key, and the backlog left by the last passes kept the shared dispatcher busy
                // after the spec, starving the next spec's refresh.
                val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

                // a cache whose loop only drains: process() is driven by hand from this thread
                val tap = ActionTap<Int, Int>()
                val cache = fastCache<Int, Int>(scope = scope, loopDelay = 20.milliseconds) { addBehaviour(tap) }
                val behaviour = FastCache.RefreshAfterWriteBehaviour<Int, Int>(refreshAfter = 1.milliseconds) {
                    loadsPerKey.incrementAndGet(it)
                    // the release path that prints nothing - the other failure path logs each key
                    throw CancellationException("refresh failed")
                }

                fun everyKeyLoaded(times: Int) = (0 until keys).all { key -> loadsPerKey.get(key) >= times }

                // Cached for real, and drained: a refresh only starts from a value that is cached and
                // has no put pending.
                (0 until keys).forEach { cache.put(it, it) }
                pollUntil { tap.actions.size >= keys } shouldBe true
                behaviour.process(cache, FastCache.ActionUpdates((0 until keys).map { FastCache.PutAction(it, it) }))

                // First round: every key is launched while failed refreshes release keys concurrently
                Thread.sleep(5)
                behaviour.process(cache, FastCache.ActionUpdates(emptyList()))

                pollUntil { everyKeyLoaded(1) } shouldBe true

                // Second round: pass again until every key was launched a second time. A key whose
                // release was merely late is launched by a later pass; one whose release was lost
                // stays claimed and is never launched again.
                try {
                    pollUntil {
                        behaviour.process(cache, FastCache.ActionUpdates(emptyList()))
                        everyKeyLoaded(2)
                    } shouldBe true
                } finally {
                    scope.cancel()
                }
            }
        }
    }
}
