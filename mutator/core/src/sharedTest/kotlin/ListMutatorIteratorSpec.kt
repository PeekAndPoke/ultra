package io.peekandpoke.mutator

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.mutator.domain.Address
import io.peekandpoke.mutator.domain.mutator

/**
 * `remove()` and `set()` of a [ListMutator]'s list iterator act on the element last returned, as `ArrayList`'s do.
 */
class ListMutatorIteratorSpec : StringSpec() {

    private val a = Address("A", "C", "Z")
    private val b = Address("B", "C", "Z")
    private val c = Address("C", "C", "Z")
    private val x = Address("X", "C", "Z")

    init {
        "reverse() on 20 elements reverses the list" {
            // The JVM's Collections.reverse() walks a list iterator backwards from 18 elements on
            val list = (1..20).map { Address("S$it", "C", "Z") }
            val mutator = list.mutator()

            mutator.reverse()

            mutator.get() shouldBe list.reversed()
        }

        "set() after previous() replaces the element previous() returned" {
            val mutator = listOf(a, b, c).mutator()
            val iterator = mutator.listIterator(2)

            iterator.previous().get() shouldBe b
            iterator.set(mutator.getChildMutator(x))

            mutator.get() shouldBe listOf(a, x, c)
        }

        "remove() after previous() removes the element previous() returned" {
            val mutator = listOf(a, b, c).mutator()
            val iterator = mutator.listIterator(2)

            iterator.previous().get() shouldBe b
            iterator.remove()

            mutator.get() shouldBe listOf(a, c)
            iterator.nextIndex() shouldBe 1
            iterator.next().get() shouldBe c
        }

        "remove() after next() removes the element next() returned" {
            val mutator = listOf(a, b, c).mutator()
            val iterator = mutator.listIterator()

            iterator.next()
            iterator.next().get() shouldBe b
            iterator.remove()

            mutator.get() shouldBe listOf(a, c)
            iterator.next().get() shouldBe c
        }

        "remove() before next() or previous() throws IllegalStateException" {
            val mutator = listOf(a, b).mutator()

            shouldThrow<IllegalStateException> { mutator.iterator().remove() }
            shouldThrow<IllegalStateException> { mutator.listIterator(1).remove() }

            mutator.get() shouldBe listOf(a, b)
        }

        "set() before next() or previous() throws IllegalStateException" {
            val mutator = listOf(a, b).mutator()

            shouldThrow<IllegalStateException> { mutator.listIterator(1).set(mutator.getChildMutator(x)) }

            mutator.get() shouldBe listOf(a, b)
        }

        "remove() twice in a row throws IllegalStateException" {
            val mutator = listOf(a, b).mutator()
            val iterator = mutator.iterator()

            iterator.next()
            iterator.remove()
            shouldThrow<IllegalStateException> { iterator.remove() }

            mutator.get() shouldBe listOf(b)
        }

        "remove() right after add() throws IllegalStateException" {
            val mutator = listOf(a, b).mutator()
            val iterator = mutator.listIterator()

            iterator.next()
            iterator.add(mutator.getChildMutator(x))
            shouldThrow<IllegalStateException> { iterator.remove() }

            mutator.get() shouldBe listOf(a, x, b)
        }
    }
}
