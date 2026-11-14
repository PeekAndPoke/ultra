package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * [NullableCache.clear] against a [NullableCache.getOrPutAsync] still running its provider.
 *
 * Every caller starts UNDISPATCHED, so it runs up to its first suspension before the test goes on:
 * the order of claim, clear and completion is fixed, with no timing involved.
 */
class NullableCacheClearSpec : StringSpec({
    timeout = TEST_TIMEOUT_MS

    "a provider still running at clear() does not cache its result" {
        val cache = NullableCache<String, String>()
        val gate = CompletableDeferred<Unit>()

        coroutineScope {
            val old = async(start = CoroutineStart.UNDISPATCHED) {
                cache.getOrPutAsync("k") { gate.await(); "old" }
            }

            cache.clear()
            gate.complete(Unit)

            // its own caller still gets the result
            old.await() shouldBe "old"
        }

        cache.has("k") shouldBe false
        cache.size shouldBe 0
    }

    "a caller after clear() runs its own provider instead of joining one from before" {
        val cache = NullableCache<String, String>()
        val gate = CompletableDeferred<Unit>()

        coroutineScope {
            val old = async(start = CoroutineStart.UNDISPATCHED) {
                cache.getOrPutAsync("k") { gate.await(); "old" }
            }

            cache.clear()

            val new = async(start = CoroutineStart.UNDISPATCHED) {
                cache.getOrPutAsync("k") { "new" }
            }

            gate.complete(Unit)

            new.await() shouldBe "new"
            old.await() shouldBe "old"
        }

        cache.get("k") shouldBe "new"
    }

    "a put while a provider runs wins over the provider's result" {
        val cache = NullableCache<String, String>()
        val gate = CompletableDeferred<Unit>()

        coroutineScope {
            val running = async(start = CoroutineStart.UNDISPATCHED) {
                cache.getOrPutAsync("k") { gate.await(); "loaded before the put" }
            }

            cache.put("k", "put meanwhile")
            gate.complete(Unit)

            // as FastCache.getOrPut: the value in the cache wins, its caller included
            running.await() shouldBe "put meanwhile"
        }

        cache.get("k") shouldBe "put meanwhile"
    }

    "a provider from before clear() that fails does not release the claim made after it" {
        val cache = NullableCache<String, String>()
        val oldGate = CompletableDeferred<Unit>()
        val newGate = CompletableDeferred<Unit>()
        var runs = 0

        coroutineScope {
            val old = async(start = CoroutineStart.UNDISPATCHED) {
                runCatching { cache.getOrPutAsync("k") { oldGate.await(); error("old provider failed") } }
            }

            cache.clear()

            val new = async(start = CoroutineStart.UNDISPATCHED) {
                cache.getOrPutAsync("k") { runs++; newGate.await(); "new" }
            }

            oldGate.complete(Unit)
            old.await().isFailure shouldBe true

            // must join the run that is in flight, not start a second one
            val third = async(start = CoroutineStart.UNDISPATCHED) {
                cache.getOrPutAsync("k") { runs++; "third" }
            }

            newGate.complete(Unit)

            new.await() shouldBe "new"
            third.await() shouldBe "new"
        }

        runs shouldBe 1
    }
})
