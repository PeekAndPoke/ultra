package io.peekandpoke.ultra.cache

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

/**
 * Every TTL runs on the cache's time source, monotonic by default - never on the wall clock.
 *
 * Each test lets real time pass well beyond the TTL while its [TestTimeSource] stands still: an entry
 * that expires then was timed by a wall clock. Then it moves the test time on.
 */
class FastCacheTimeSourceSpec : StringSpec({

    "expireAfterWrite runs on the cache's time source" {
        val time = TestTimeSource()
        val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
            timeSource(time)
            expireAfterWrite(100.milliseconds)
        }

        cache.put("a", "1")
        delay(300.milliseconds)
        cache.keys shouldBe setOf("a")

        time += 150.milliseconds
        eventually(3.seconds) { cache.keys shouldBe emptySet() }
    }

    "expireAfterAccess runs on the cache's time source" {
        val time = TestTimeSource()
        val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
            timeSource(time)
            expireAfterAccess(100.milliseconds)
        }

        cache.put("a", "1")
        delay(300.milliseconds)
        cache.keys shouldBe setOf("a")

        time += 150.milliseconds
        eventually(3.seconds) { cache.keys shouldBe emptySet() }
    }

    "refreshAfterWrite and its hard TTL run on the cache's time source" {
        val time = TestTimeSource()
        var loads = 0
        val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
            timeSource(time)
            refreshAfterWrite(100.milliseconds, hardTtl = 1.seconds) { loads++; "loaded" }
        }

        cache.put("a", "1")
        delay(300.milliseconds)
        loads shouldBe 0

        time += 150.milliseconds
        eventually(3.seconds) { cache.get("a") shouldBe "loaded" }

        // let the loop stamp the refreshed value (its put) before the clock moves on
        delay(100.milliseconds)

        // past the hard TTL of the refreshed value: evicted, not refreshed
        val loadsBefore = loads
        time += 2.seconds
        eventually(3.seconds) { cache.keys shouldBe emptySet() }
        loads shouldBe loadsBefore
    }
})
