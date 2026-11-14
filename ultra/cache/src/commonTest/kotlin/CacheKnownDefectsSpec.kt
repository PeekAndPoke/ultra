package io.peekandpoke.ultra.cache

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Executable statements of the design questions from the ultra/cache scan — written as FAILING
 * tests before the fixes were chosen, green since, and kept as the guards for those fixes. See
 * ULTRA-00159 (cache scan findings) for what each one measured while red.
 *
 * Each test asserts what the documented contract already promises — none of them invents a new
 * requirement.
 */
class CacheKnownDefectsSpec : StringSpec() {

    init {
        timeout = TEST_TIMEOUT_MS

        // Q1 - eviction is not announced to other behaviours //////////////////////////////////////

        "Q1a: a key evicted by maxEntries is dropped by refreshAfterWrite" {
            // The refresh behaviour is driven by hand with what the cache recorded. Asserting only that
            // the loader is never called stopped guarding this: a refresh now skips a key that is not
            // cached, so only a key that comes back shows a write time left behind.
            val tap = ActionTap<String, String>()
            val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
                maxEntries(1)
                addBehaviour(tap)
            }
            var loads = 0
            val refresh = FastCache.RefreshAfterWriteBehaviour<String, String>(100.milliseconds) { loads++; it }

            cache.put("a", "1")
            eventually(2.seconds) {
                tap.actions.any { it is FastCache.PutAction && it.key == "a" } shouldBe true
            }
            refresh.process(cache, FastCache.ActionUpdates(tap.takeNew()))

            cache.put("b", "2")

            // "a" was evicted by maxEntries. When removeSilently recorded no action, the refresh
            // behaviour kept its write time and reloaded it - then maxEntries evicted it again,
            // forever, hammering the backing store.
            eventually(2.seconds) {
                tap.actions.any { it is FastCache.RemoveAction && it.key == "a" } shouldBe true
            }
            refresh.process(cache, FastCache.ActionUpdates(tap.takeNew()))

            refreshStillTracksA(cache, refresh) { loads } shouldBe false
        }

        "Q1b: totalSize stops counting an entry another behaviour evicted" {
            val shared = FastCache.MaxMemoryUsageBehaviour<String, String>(maxMemorySize = 1_000_000)
            val reference = FastCache.MaxMemoryUsageBehaviour<String, String>(maxMemorySize = 1_000_000)

            val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
                maxEntries(1)
                addBehaviour(shared)
            }

            // holds exactly the one entry that survives eviction above
            val referenceCache = fastCache<String, String>(loopDelay = 20.milliseconds) {
                addBehaviour(reference)
            }

            cache.put("a", "1")
            cache.put("b", "2")
            referenceCache.put("b", "2")

            // Until both loops have run: a fixed 400 ms was too short under load
            eventually(10.seconds) {
                cache.size shouldBe 1
                referenceCache.size shouldBe 1
                (reference.totalSize > 0L) shouldBe true

                // Both caches hold one entry, so the accounted memory must match. It did not when an
                // eviction was not announced: the evicted "a" was still counted.
                shared.totalSize shouldBe reference.totalSize
            }

            keepReachable(cache)
            keepReachable(referenceCache)
        }

        // Q2 - null values are stored but invisible ///////////////////////////////////////////////

        "Q2: a null value is retrievable, or it must not be stored at all" {
            val cache = fastCache<String, String?>(loopDelay = 20.milliseconds) {}

            cache.put("k", null)

            // The entry occupies a slot...
            cache.size shouldBe 1
            cache.keys.contains("k") shouldBe true

            // ...but every accessor denies it exists, because `map[key] != null` is the presence
            // test throughout. size/keys and has/get therefore contradict each other.
            cache.has("k") shouldBe true

            var producerCalls = 0
            cache.getOrPut("k") {
                producerCalls++
                "computed"
            }

            // Re-running the producer for a key that is present is the practical consequence:
            // negative caching never works for a nullable-V cache.
            producerCalls shouldBe 0
        }

        // Q4 - NullableCache holds one non-reentrant Mutex across the provider ////////////////////

        "Q4: getOrPutAsync does not deadlock when the provider resolves another key" {
            val subject = NullableCache<String, String>()

            val result = withTimeout(3.seconds) {
                subject.getOrPutAsync("outer") {
                    // Nested resolution is the normal shape for vault's RefCodec path: a Ref inside
                    // a Ref. kotlinx Mutex is not reentrant, so this suspends forever.
                    subject.getOrPutAsync("inner") { "inner-value" }

                    "outer-value"
                }
            }

            result shouldBe "outer-value"
        }

        // Q5 - a hard TTL does not survive an in-flight refresh ///////////////////////////////////

        "Q5: an entry hard-evicted during a refresh does not come back when the loader returns" {
            val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
                refreshAfterWrite(ttl = 100.milliseconds, hardTtl = 200.milliseconds) {
                    // still running when the hard TTL fires
                    delay(500.milliseconds)
                    "reloaded"
                }
            }

            cache.put("a", "1")

            // Polling, not a single late assertion: the resurrected entry gets a FRESH write
            // timestamp, so the hard TTL evicts it again ~200ms later and a late snapshot shows it
            // absent again. The defect is that it comes back at all.
            var reappearedAfterHardEviction = false

            repeat(30) { tick ->
                delay(50.milliseconds)

                // from 400ms on, the hard TTL (200ms) has long since evicted it
                if (tick >= 8 && cache.keys.contains("a")) {
                    reappearedAfterHardEviction = true
                }
            }

            reappearedAfterHardEviction shouldBe false
        }
    }
}

