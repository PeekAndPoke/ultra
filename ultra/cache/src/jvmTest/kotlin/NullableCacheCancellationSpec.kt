package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread

/**
 * A caller waiting on another caller's provider in [NullableCache.getOrPutAsync] and then cancelled.
 *
 * Each scenario runs in `runBlocking` on a thread of its own: should the cancelled caller spin without
 * suspending, it blocks that single thread for good - a `withTimeout` there could never fire - and
 * the test sees the thread still alive. The thread is a daemon, so it dies with the test JVM.
 */
class NullableCacheCancellationSpec : StringSpec({
    timeout = TEST_TIMEOUT_MS

    fun finishesWithin(millis: Long, block: suspend () -> Unit): Boolean {
        val worker = thread(isDaemon = true) { runBlocking { block() } }

        worker.join(millis)

        return !worker.isAlive
    }

    "a cancelled joiner stops waiting and lets the owner finish" {
        var ownerResult: String? = null
        var joinerCancelled = false

        val finished = finishesWithin(5_000) {
            val cache = NullableCache<String, String>()
            val gate = CompletableDeferred<Unit>()

            coroutineScope {
                val owner = async(start = CoroutineStart.UNDISPATCHED) {
                    cache.getOrPutAsync("k") { gate.await(); "owner" }
                }

                val joiner = launch(start = CoroutineStart.UNDISPATCHED) {
                    try {
                        cache.getOrPutAsync("k") { "joiner" }
                    } catch (e: CancellationException) {
                        joinerCancelled = true
                        throw e
                    }
                }

                joiner.cancel()
                joiner.join()

                gate.complete(Unit)
                ownerResult = owner.await()
            }
        }

        finished shouldBe true
        joinerCancelled shouldBe true
        ownerResult shouldBe "owner"
    }

    "a joiner is not cancelled when the owner's provider is" {
        var joinerResult: String? = null

        val finished = finishesWithin(5_000) {
            val cache = NullableCache<String, String>()
            val gate = CompletableDeferred<Unit>()

            coroutineScope {
                val owner = launch(start = CoroutineStart.UNDISPATCHED) {
                    cache.getOrPutAsync("k") { gate.await(); "owner" }
                }

                val joiner = async(start = CoroutineStart.UNDISPATCHED) {
                    cache.getOrPutAsync("k") { "joiner" }
                }

                owner.cancel()

                // the owner's cancellation is not the joiner's: it takes over and runs its own provider
                joinerResult = joiner.await()
            }
        }

        finished shouldBe true
        joinerResult shouldBe "joiner"
    }
})
