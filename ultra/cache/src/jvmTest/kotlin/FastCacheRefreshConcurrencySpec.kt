package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

/**
 * The refresh bookkeeping of [FastCache.RefreshAfterWriteBehaviour] under real threads (F4).
 *
 * A refresh that fails releases its key from the "refreshing" set on the thread the loader ran on,
 * while `process()` adds keys to that set on the loop thread. With an unsynchronised set, a release
 * lost to a concurrent resize leaves the key marked as refreshing forever: never refreshed again.
 */
class FastCacheRefreshConcurrencySpec : StringSpec() {

    init {
        "a key released by a failed refresh on another thread is refreshed again" {
            val keys = 20_000

            // The release is lost mostly while the set grows, so each trial starts with a fresh one.
            // One trial misses the race about one run in five; five trials make a miss very unlikely.
            repeat(5) {
                val loads = AtomicInteger(0)

                // a cache without behaviours: process() is driven by hand from this thread
                val cache = fastCache<Int, Int>(loopDelay = 1_000.milliseconds) {}
                val behaviour = FastCache.RefreshAfterWriteBehaviour<Int, Int>(refreshAfter = 1.milliseconds) {
                    loads.incrementAndGet()
                    // the release path that prints nothing - the other failure path logs each key
                    throw CancellationException("refresh failed")
                }

                behaviour.process(cache, FastCache.ActionUpdates((0 until keys).map { FastCache.PutAction(it, it) }))

                // first round: every key is launched while failed refreshes release keys concurrently;
                // second round: every key must have been released, so every key is launched again
                listOf(keys, 2 * keys).forEach { expected ->
                    Thread.sleep(5)

                    behaviour.process(cache, FastCache.ActionUpdates(emptyList()))

                    val deadline = System.currentTimeMillis() + 10_000

                    while (loads.get() < expected && System.currentTimeMillis() < deadline) {
                        Thread.sleep(10)
                    }

                    // the key is released right after the load is counted
                    Thread.sleep(100)

                    loads.get() shouldBe expected
                }
            }
        }
    }
}
