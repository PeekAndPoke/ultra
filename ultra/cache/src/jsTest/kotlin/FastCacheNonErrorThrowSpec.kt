package io.peekandpoke.ultra.cache

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Throws a JS value that is no `Error` - which `catch (e: Throwable)` lets through. */
private fun throwNonError(): Nothing {
    js("(function() { throw 'not an Error'; })")()
    error("unreachable")
}

/** User code throwing a value that is no `Error` does not end the processing loop. */
class FastCacheNonErrorThrowSpec : StringSpec({

    timeout = TEST_TIMEOUT_MS

    "a size estimator throwing a non-Error value" {
        val estimator = object : ObjectSizeEstimator {
            override fun estimate(obj: Any?): Long = if (obj == "bad") throwNonError() else 1L
        }
        val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
            maxMemoryUsage(1_000_000, estimator)
            expireAfterWrite(100.milliseconds)
        }

        cache.put("bad", "x")
        cache.put("k", "v")

        // expiry needs the loop: it still runs after the estimator's throw
        eventually(5.seconds) { cache.size shouldBe 0 }

        // driven by hand: the batch goes on after the failing entry, which is charged the fallback
        val behaviour = FastCache.MaxMemoryUsageBehaviour<String, String>(1_000_000, estimator)
        behaviour.process(
            cache,
            FastCache.ActionUpdates(listOf(FastCache.PutAction("bad", "x"), FastCache.PutAction("k", "v"))),
        )
        behaviour.totalSize shouldBe FastCache.MaxMemoryUsageBehaviour.DEFAULT_FALLBACK_SIZE + 2L

        keepReachable(cache)
    }

    "a behaviour throwing a non-Error value" {
        val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
            addBehaviour(object : FastCache.Behaviour<String, String> {
                override fun process(
                    cache: FastCache<String, String>,
                    updates: FastCache.ActionUpdates<String, String>,
                ) {
                    throwNonError()
                }
            })
            expireAfterWrite(50.milliseconds)
        }

        cache.put("k", "v")

        // the behaviour after it still runs, every pass
        eventually(5.seconds) { cache.size shouldBe 0 }

        keepReachable(cache)
    }

    "an eviction listener throwing a non-Error value" {
        var calls = 0
        val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
            expireAfterWrite(50.milliseconds)
            onEviction { _, _ -> throwNonError() }
            onEviction { _, _ -> calls++ }
        }

        cache.put("a", "1")
        eventually(5.seconds) { cache.size shouldBe 0 }

        // a later entry expires as well: the loop survived; the listener behind the throwing one ran
        cache.put("b", "2")
        eventually(5.seconds) { calls shouldBe 2 }
        cache.size shouldBe 0

        keepReachable(cache)
    }

    "a refresh loader throwing a non-Error value" {
        var calls = 0
        val scope = CoroutineScope(SupervisorJob())
        val cache = fastCache<String, String>(scope = scope, loopDelay = 20.milliseconds) {
            refreshAfterWrite(ttl = 20.milliseconds, hardTtl = 60.seconds) {
                calls++
                throwNonError()
            }
        }

        cache.put("k", "v")

        // a failed refresh is retried every pass: the loop and the retries go on
        eventually(5.seconds) { (calls >= 3) shouldBe true }
        cache.get("k") shouldBe "v"

        // each failed refresh completes its coroutine: an escaped throw left it active for good, so
        // the scope gathered one stuck job per pass besides the loop
        eventually(5.seconds) { scope.coroutineContext.job.children.count() shouldBe 1 }

        keepReachable(cache)
        scope.cancel()
    }
})
