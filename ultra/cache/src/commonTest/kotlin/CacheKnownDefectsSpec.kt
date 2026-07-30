package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Executable statements of the design questions from the ultra/cache scan — written as FAILING
 * tests before the fixes were chosen, green since, and kept as the guards for those fixes. See
 * `.claude/tasks/20260730-cache-scan-findings.md` for what each one measured while red.
 *
 * Each test asserts what the documented contract already promises — none of them invents a new
 * requirement.
 */
class CacheKnownDefectsSpec : StringSpec() {

    init {

        // Q1 - eviction is not announced to other behaviours //////////////////////////////////////

        "Q1a: a key evicted by maxEntries is not resurrected by refreshAfterWrite" {
            val loadedKeys = mutableListOf<String>()

            val cache = fastCache<String, String>(loopDelay = 20.milliseconds) {
                maxEntries(1)
                refreshAfterWrite(100.milliseconds) { key ->
                    loadedKeys.add(key)
                    "reloaded-$key"
                }
            }

            cache.put("a", "1")
            delay(60.milliseconds)
            cache.put("b", "2")

            // several refresh windows
            delay(600.milliseconds)

            // "a" was evicted by maxEntries. removeSilently records no action, so the refresh
            // behaviour still holds its write timestamp and reloads it - then maxEntries evicts
            // again, forever, hammering the backing store.
            loadedKeys shouldNotContain "a"
            cache.keys shouldNotContain "a"
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

            delay(400.milliseconds)

            cache.size shouldBe 1
            referenceCache.size shouldBe 1

            // Both caches hold one entry, so the accounted memory must match. It does not: the
            // evicted "a" is still counted, and the cap is then enforced against freed bytes.
            shared.totalSize shouldBe reference.totalSize
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

