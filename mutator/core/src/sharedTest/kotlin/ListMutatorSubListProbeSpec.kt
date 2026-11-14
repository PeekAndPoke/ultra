package io.peekandpoke.mutator

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.mutator.domain.Address
import io.peekandpoke.mutator.domain.mutator

/**
 * Stdlib functions over a [ListMutator] that do or do not go through [ListMutator.subList] (ULTRA-00234).
 *
 * `chunked` and `windowed` do not call `subList`; `slice(IntRange)` does. `subList` itself is pinned in
 * [ListMutatorSubListSpec].
 */
class ListMutatorSubListProbeSpec : StringSpec() {

    private val a = Address("A", "C", "Z")
    private val b = Address("B", "C", "Z")
    private val c = Address("C", "C", "Z")

    init {
        "chunked() groups the child mutators in order" {
            val mutator = listOf(a, b, c).mutator()

            mutator.chunked(2).map { chunk -> chunk.map { it.get() } } shouldBe listOf(listOf(a, b), listOf(c))
        }

        "windowed() returns the overlapping windows of child mutators" {
            val mutator = listOf(a, b, c).mutator()

            mutator.windowed(2).map { window -> window.map { it.get() } } shouldBe listOf(listOf(a, b), listOf(b, c))
        }

        "windowed() with partial windows keeps the short last window" {
            val mutator = listOf(a, b, c).mutator()

            val windows = mutator.windowed(size = 2, step = 2, partialWindows = true)

            windows.map { window -> window.map { it.get() } } shouldBe listOf(listOf(a, b), listOf(c))
        }

        "slice(IntRange) returns the child mutators of the range" {
            val mutator = listOf(a, b, c).mutator()

            mutator.slice(0..1).map { it.get() } shouldBe listOf(a, b)
            mutator.slice(1..2).map { it.get() } shouldBe listOf(b, c)
            mutator.slice(IntRange.EMPTY) shouldBe emptyList()
        }
    }
}
