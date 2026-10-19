package io.peekandpoke.funktor.cluster.locks

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.datetime.Kronos
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.merge
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Suppress("EXPERIMENTAL_API_USAGE")
abstract class GlobalLockProviderSpecBase : StringSpec() {

    abstract fun createSubject(): GlobalLocksProvider

    /** Collects this flow only once [holding] completed: it must contend for a lock that is held, not race for it. */
    private fun <T> Flow<T>.onceHeld(holding: CompletableDeferred<Unit>): Flow<T> = flow {
        holding.await()
        emitAll(this@onceHeld)
    }

    init {

        "Locking must work" {

            val subject = createSubject()
            val key = "key"
            val holding = CompletableDeferred<Unit>()

            val flow1 = subject.acquire(key) {
                holding.complete(Unit)
                delay(10)
                "1st"
            }

            val flow2 = subject.acquire(key, 1.seconds) {
                delay(10)
                "2nd"
            }.onceHeld(holding)

            val result = merge(flow1, flow2).fold(emptyList<String>()) { acc, it -> acc.plus(it) }

            result shouldBe listOf("1st", "2nd")
        }

        "Locking different keys must work" {

            val subject = createSubject()

            val flow1 = subject.acquire("key-1") {
                delay(100)
                "1st"
            }

            val flow2 = subject.acquire("key-2") {
                delay(10)
                "2nd"
            }

            val result = merge(flow1, flow2).fold(emptyList<String>()) { acc, it -> acc.plus(it) }

            result shouldBe listOf("2nd", "1st")
        }

        "Nested locking must work" {

            val subject = createSubject()

            val results = mutableListOf<String>()

            subject.tryToLock("key-1", 10.seconds) {

                subject.tryToLock("key-2", 10.seconds) {

                    subject.tryToLock("key-3", 10.seconds) {
                        results.add("key-3")
                    }

                    delay(10.milliseconds)
                    results.add("key-2")
                }

                delay(10.milliseconds)
                results.add("key-1")
            }

            eventually(1.seconds) {
                results shouldBe listOf("key-3", "key-2", "key-1")
            }
        }

        "Waiting for a previous lock to be freed must work" {

            val subject = createSubject()

            val key = "key"
            val holding = CompletableDeferred<Unit>()

            val flow1 = subject.acquire(key) {
                holding.complete(Unit)
                delay(100)
                "1st"
            }

            val flow2 = subject.acquire(key, 1.seconds) {
                "2nd"
            }.onceHeld(holding)

            val result = merge(flow1, flow2)
                .fold(emptyList<String>()) { acc, it -> acc.plus(it) }

            result shouldBe listOf("1st", "2nd")
        }

        "Lock timeout must be respected" {

            val subject = PrimitiveGlobalLocksProvider()

            val key = "key"

            val flow1 = subject.acquire(key) {
                delay(1.seconds)
            }

            val timeout = 100.milliseconds
            val start = Kronos.systemUtc.instantNow()

            val flow2 = subject.acquire(key, timeout) {}

            eventually(1.seconds) {
                shouldThrow<LocksException.Timeout> {
                    merge(flow1, flow2).collect()
                }
            }

            val end = Kronos.systemUtc.instantNow()
            val timePassed = end - start

            timePassed shouldBeGreaterThanOrEqualTo timeout
            timePassed shouldBeLessThan timeout * 2
        }

        "Reentering a lock on the same instance must fail" {

            val inst = createSubject()

            val key = "key"
            val holding = CompletableDeferred<Unit>()

            val flow1 = inst.acquire(key) {
                holding.complete(Unit)
                delay(100)
                "1st"
            }

            val flow2 = inst.acquire(key, 1.milliseconds) {
                delay(10)
                "2nd"
            }.onceHeld(holding).catch {
                emit("2nd failed")
            }

            val result = merge(flow1, flow2).fold(emptyList<String>()) { acc, it -> acc.plus(it) }

            result shouldBe listOf("2nd failed", "1st")
        }

        "Locking timeout must work, when using two different instances" {

            val one = createSubject()
            val two = createSubject()

            val key = "key"
            val holding = CompletableDeferred<Unit>()

            val flow1 = one.acquire(key) {
                holding.complete(Unit)
                delay(100)
                "1st"
            }

            val flow2 = two.acquire(key, 1.milliseconds) {
                delay(10)
                "2nd"
            }.onceHeld(holding).catch {
                emit("2nd failed")
            }

            val result = merge(flow1, flow2)
                .fold(emptyList<String>()) { acc, it -> acc.plus(it) }

            result shouldBe listOf("2nd failed", "1st")
        }

        "Handler throwing an error" {

            val subject = createSubject()
            val key = "key"
            val holding = CompletableDeferred<Unit>()
            val firstFailed = AtomicBoolean(false)

            val flow1 = subject.acquire<String>(key) {
                holding.complete(Unit)
                delay(100)
                firstFailed.set(true)
                error("1st failed")
            }.catch { e ->
                emit("error: ${e.message}")
            }

            val flow2 = subject.acquire(key, 1.seconds) {
                // Only once the first handler has failed: it held the lock until then.
                if (firstFailed.get()) "2nd" else "2nd, while the 1st still ran"
            }.onceHeld(holding).catch { e ->
                emit("error: ${e.message}")
            }

            val result = merge(flow1, flow2)
                .fold(emptyList<String>()) { acc, it -> acc.plus(it) }

            // The error is emitted after the release, so the second handler may come first.
            result shouldContainExactlyInAnyOrder listOf("error: 1st failed", "2nd")
        }
    }
}
