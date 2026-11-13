package io.peekandpoke.mutator

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.mutator.domain.Address
import io.peekandpoke.mutator.domain.city
import io.peekandpoke.mutator.domain.mutator
import io.peekandpoke.mutator.domain.street

/**
 * Changes through a child mutator of a [ListMutator] write back to the right element, every time.
 */
class ListMutatorChildWriteBackSpec : StringSpec() {

    private val a = Address("A", "C", "Z")
    private val b = Address("B", "C", "Z")
    private val c = Address("C", "C", "Z")

    init {
        "two changes through one held element both land" {
            val mutator = listOf(a, b).mutator()
            var notified = 0
            mutator.onChange { notified++ }

            val element = mutator[0]
            element.street = "A2"
            element.city = "C2"

            mutator.get() shouldBe listOf(Address("A2", "C2", "Z"), b)
            notified shouldBe 2
        }

        "two changes through one snapshot element both land" {
            val mutator = listOf(a, b).mutator()
            var notified = 0
            mutator.onChange { notified++ }

            val snapshot = mutator.subList(0, 2)
            snapshot[0].street = "A2"
            snapshot[0].city = "C2"

            mutator.get() shouldBe listOf(Address("A2", "C2", "Z"), b)
            notified shouldBe 2
        }

        "two changes through one iterator element both land" {
            val mutator = listOf(a, b).mutator()

            val iterator = mutator.listIterator()
            iterator.next()
            val element = iterator.next()
            element.street = "B2"
            element.city = "C2"

            iterator.previous().street = "B3"

            mutator.get() shouldBe listOf(a, Address("B3", "C2", "Z"))
        }

        "two changes per element in forEach both land" {
            val mutator = listOf(a, b).mutator()

            mutator.forEach {
                it.street += "2"
                it.city += "2"
            }

            mutator.get() shouldBe listOf(Address("A2", "C2", "Z"), Address("B2", "C2", "Z"))
        }

        "a change through a held element goes to its own index when the same instance is in the list twice" {
            val mutator = listOf(a, a).mutator()

            mutator[1].street = "X"

            mutator.get() shouldBe listOf(a, a.copy(street = "X"))
        }

        "a change through a snapshot element goes to its own index when the same instance is in the list twice" {
            val mutator = listOf(a, a).mutator()

            mutator.subList(1, 2)[0].street = "X"

            mutator.get() shouldBe listOf(a, a.copy(street = "X"))
        }

        "a change through an iterator element goes to its own index when the same instance is in the list twice" {
            val mutator = listOf(a, a).mutator()

            mutator.listIterator(1).next().street = "X"

            mutator.get() shouldBe listOf(a, a.copy(street = "X"))
        }

        "a change finds its element by identity after the element moved" {
            val mutator = listOf(a, b, c).mutator()
            val element = mutator[2]

            // c moves from index 2 to 1, and index 2 now holds a different element
            mutator.removeAt(0)
            mutator.add(a)
            element.street = "C2"

            mutator.get() shouldBe listOf(b, c.copy(street = "C2"), a)
        }

        "a change through an element the list no longer holds is dropped" {
            val mutator = listOf(a, b).mutator()
            val element = mutator[0]

            mutator.removeAt(0)
            var notified = 0
            mutator.onChange { notified++ }
            element.street = "A2"

            mutator.get() shouldBe listOf(b)
            notified shouldBe 0
        }
    }
}
