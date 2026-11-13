package io.peekandpoke.mutator

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.mutator.domain.Address
import io.peekandpoke.mutator.domain.mutator

/**
 * Which stdlib functions reach the throwing [ListMutator.subList] (ULTRA-00234).
 *
 * `chunked` and `windowed` do not call `subList`; `slice(IntRange)` does. `subList` itself is pinned in
 * [ListMutatorSpec].
 */
class ListMutatorSubListProbeSpec : StringSpec() {

    private val a = Address("A", "C", "Z")
    private val b = Address("B", "C", "Z")
    private val c = Address("C", "C", "Z")

    init {
        "chunked() does not reach subList()" {
            val mutator = listOf(a, b, c).mutator()

            mutator.chunked(2).map { chunk -> chunk.map { it.get() } } shouldBe listOf(listOf(a, b), listOf(c))
        }

        "windowed() does not reach subList()" {
            val mutator = listOf(a, b, c).mutator()

            mutator.windowed(2).map { window -> window.map { it.get() } } shouldBe listOf(listOf(a, b), listOf(b, c))
        }

        "windowed() with partial windows does not reach subList()" {
            val mutator = listOf(a, b, c).mutator()

            mutator.windowed(size = 2, step = 2, partialWindows = true).map { window -> window.map { it.get() } } shouldBe
                listOf(listOf(a, b), listOf(c))
        }

        "slice(IntRange) reaches subList() and throws" {
            val mutator = listOf(a, b, c).mutator()

            shouldThrow<UnsupportedOperationException> {
                mutator.slice(0..1)
            }
        }
    }
}
