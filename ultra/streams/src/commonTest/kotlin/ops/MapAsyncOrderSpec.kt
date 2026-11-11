package io.peekandpoke.ultra.streams.ops

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.streams.StreamSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/** `mapAsync`: the latest value wins, whatever order the mappings finish in (ULTRA-00308). */
class MapAsyncOrderSpec : StringSpec({

    "a slow earlier mapping finishing last is not published" {
        val gates = mapOf(1 to CompletableDeferred<String>(), 2 to CompletableDeferred())
        val source = StreamSource(0)
        val mapped = source.mapAsync(scope = CoroutineScope(Dispatchers.Unconfined)) { gates[it]?.await() ?: "zero" }
        val seen = mutableListOf<String?>()

        mapped.subscribeToStream { seen.add(it) }
        source(1)
        source(2)

        gates.getValue(2).complete("two")
        gates.getValue(1).complete("one")

        seen shouldBe listOf("zero", "two")
        mapped() shouldBe "two"
    }
})
