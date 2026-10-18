package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * Single-flight behaviour of [NullableCache] under real contention.
 *
 * `NullableCacheSpec` is single-threaded, so it exercises only the fast path — which handles the
 * null sentinel correctly. The defect lived in the double-check *inside* the lock, and that branch
 * is unreachable without a race.
 */
class NullableCacheConcurrencySpec : StringSpec({

    "getOrPut runs the provider once for a NULL result, even when callers pile up on the lock" {
        val threadCount = 8
        val start = CountDownLatch(1)
        val calls = AtomicInteger(0)

        val subject = NullableCache<String, String>()

        // All threads are released together, so they all clear the fast read (the key is absent, and
        // nothing is stored until the first provider returns) and then queue on the lock. The first
        // one caches the miss; the rest enter the lock afterwards and hit the double-check.
        val threads = (1..threadCount).map {
            thread {
                start.await()

                subject.getOrPut("key") {
                    calls.incrementAndGet()
                    // long enough that every other thread is parked on the lock before this returns
                    Thread.sleep(50)
                    null
                }
            }
        }

        start.countDown()
        threads.forEach { it.join() }

        // Branching on the decoded value instead of on presence made each queued caller re-run the
        // provider, because the MISSING sentinel decodes to null
        calls.get() shouldBe 1

        subject.has("key") shouldBe true
        subject.get("key") shouldBe null
    }

    "getOrPutAsync runs the provider once per key for concurrent callers" {
        val subject = NullableCache<String, String>()
        val calls = AtomicInteger(0)

        // The per-key single-flight replaced a process-wide Mutex. The Mutex guaranteed this for
        // free; the replacement has to be shown to still do it, or the deadlock fix traded one
        // defect for another.
        val results = coroutineScope {
            (1..8).map {
                async {
                    subject.getOrPutAsync("key") {
                        calls.incrementAndGet()
                        delay(50)
                        "value"
                    }
                }
            }.awaitAll()
        }

        calls.get() shouldBe 1
        results.all { it == "value" } shouldBe true
    }

    "getOrPutAsync runs the provider once per key for a NULL result too" {
        val subject = NullableCache<String, String>()
        val calls = AtomicInteger(0)

        coroutineScope {
            (1..8).map {
                async {
                    subject.getOrPutAsync("key") {
                        calls.incrementAndGet()
                        delay(50)
                        null
                    }
                }
            }.awaitAll()
        }

        calls.get() shouldBe 1
        subject.has("key") shouldBe true
    }

    "a failing provider does not poison the key for later callers" {
        val subject = NullableCache<String, String>()

        // The owner removes its in-flight marker on failure, so a joiner must be able to take over
        // rather than inheriting an exception raised on someone else's call stack.
        runCatching {
            subject.getOrPutAsync("key") { error("provider blew up") }
        }.isFailure shouldBe true

        subject.has("key") shouldBe false
        subject.getOrPutAsync("key") { "recovered" } shouldBe "recovered"
    }

    "getOrPut runs the provider once for a non-null result under the same contention" {
        val threadCount = 8
        val start = CountDownLatch(1)
        val calls = AtomicInteger(0)

        val subject = NullableCache<String, String>()

        val threads = (1..threadCount).map {
            thread {
                start.await()

                subject.getOrPut("key") {
                    calls.incrementAndGet()
                    Thread.sleep(50)
                    "value"
                }
            }
        }

        start.countDown()
        threads.forEach { it.join() }

        // This half always worked - it is here so a regression can be localised to the null path
        calls.get() shouldBe 1
        subject.get("key") shouldBe "value"
    }
})
