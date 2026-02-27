package de.peekandpoke.mutator

import de.peekandpoke.mutator.domain.Address
import de.peekandpoke.mutator.domain.SealedInterface
import de.peekandpoke.mutator.domain.mutate
import de.peekandpoke.mutator.domain.mutator
import de.peekandpoke.mutator.domain.street
import de.peekandpoke.mutator.domain.value
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ListMutatorSpec : StringSpec() {

    init {
        "should mutate list" {
            val list = listOf(
                Address("Street 1", "City 1", "Zip 1"),
                Address("Street 2", "City 2", "Zip 2")
            )

            val mutated = list.mutate {
                forEach {
                    it.street += " Mutated"
                }
            }

            mutated shouldBe listOf(
                Address("Street 1 Mutated", "City 1", "Zip 1"),
                Address("Street 2 Mutated", "City 2", "Zip 2")
            )
        }

        "forEach should iterate over all mutators" {
            val list = listOf(
                Address("A", "City", "Zip"),
                Address("B", "City", "Zip")
            )

            var count = 0
            val mutated = list.mutate {
                forEach { mutator ->
                    count++
                    mutator.street += count.toString()
                }
            }

            count shouldBe 2
            mutated shouldBe listOf(
                Address("A1", "City", "Zip"),
                Address("B2", "City", "Zip")
            )
        }

        "forEachIndexed should iterate over all mutators with indices" {
            val list = listOf(
                Address("A", "City", "Zip"),
                Address("B", "City", "Zip")
            )

            val mutated = list.mutate {
                forEachIndexed { index, mutator ->
                    mutator.street += index.toString()
                }
            }

            mutated shouldBe listOf(
                Address("A0", "City", "Zip"),
                Address("B1", "City", "Zip")
            )
        }

        "map should transform mutators" {
            val list = listOf(
                Address("A", "City", "Zip"),
                Address("B", "City", "Zip")
            )

            val mutated = list.mutate {
                forEach { it.street += "!" }
            }

            mutated shouldBe listOf(
                Address("A!", "City", "Zip"),
                Address("B!", "City", "Zip")
            )
        }

        "mutator().forEach should iterate and mutate correctly" {
            val list = listOf(
                Address("A", "City", "Zip"),
                Address("B", "City", "Zip")
            )

            val mutator = list.mutator()

            mutator.forEach {
                it.street += "!"
            }

            mutator.get() shouldBe listOf(
                Address("A!", "City", "Zip"),
                Address("B!", "City", "Zip")
            )
        }

        "mutator().forEachIndexed should iterate and mutate correctly" {
            val list = listOf(
                Address("A", "City", "Zip"),
                Address("B", "City", "Zip")
            )

            val mutator = list.mutator()

            mutator.forEachIndexed { index, item ->
                item.street += index.toString()
            }

            mutator.get() shouldBe listOf(
                Address("A0", "City", "Zip"),
                Address("B1", "City", "Zip")
            )
        }

        "mutator().forEach should work with a list of sealed interfaces" {
            val list = listOf(
                SealedInterface.One("1"),
                SealedInterface.Two(2)
            )

            val mutator = list.mutator()

            mutator.forEach { item ->
                when (val v = item.get()) {
                    is SealedInterface.One -> item.cast(v) { value += "-changed" }
                    is SealedInterface.Two -> item.cast(v) { value += 10 }
                }
            }

            mutator.get() shouldBe listOf(
                SealedInterface.One("1-changed"),
                SealedInterface.Two(12)
            )
        }

        "mutator().forEachIndexed should work with a list of sealed interfaces" {
            val list = listOf(
                SealedInterface.One("1"),
                SealedInterface.Two(2)
            )

            val mutator = list.mutator()

            mutator.forEachIndexed { index, item ->
                when (val v = item.get()) {
                    is SealedInterface.One -> item.cast(v) { value += "-$index" }
                    is SealedInterface.Two -> item.cast(v) { value += index }
                }
            }

            mutator.get() shouldBe listOf(
                SealedInterface.One("1-0"),
                SealedInterface.Two(3)
            )
        }

        "iterator should support hasNext and next" {
            val list = listOf(
                Address("A", "C", "Z"),
                Address("B", "C", "Z")
            )

            val mutator = list.mutator()
            val iterator = mutator.iterator()

            iterator.hasNext() shouldBe true
            iterator.next().get().street shouldBe "A"

            iterator.hasNext() shouldBe true
            iterator.next().get().street shouldBe "B"

            iterator.hasNext() shouldBe false
        }

        "listIterator should support nextIndex and previousIndex" {
            val list = listOf(
                Address("A", "C", "Z"),
                Address("B", "C", "Z")
            )

            val mutator = list.mutator()
            val iterator = mutator.listIterator()

            iterator.nextIndex() shouldBe 0
            iterator.previousIndex() shouldBe -1

            iterator.next()
            iterator.nextIndex() shouldBe 1
            iterator.previousIndex() shouldBe 0

            iterator.next()
            iterator.nextIndex() shouldBe 2
            iterator.previousIndex() shouldBe 1
        }

        "listIterator should support hasPrevious and previous" {
            val list = listOf(
                Address("A", "C", "Z"),
                Address("B", "C", "Z")
            )

            val mutator = list.mutator()
            val iterator = mutator.listIterator(2)

            iterator.hasPrevious() shouldBe true
            iterator.previous().get().street shouldBe "B"

            iterator.hasPrevious() shouldBe true
            iterator.previous().get().street shouldBe "A"

            iterator.hasPrevious() shouldBe false
        }

        "listIterator should support remove" {
            val list = listOf(
                Address("A", "C", "Z"),
                Address("B", "C", "Z"), Address("C", "C", "Z")
            )

            val mutator = list.mutator()
            val iterator = mutator.listIterator()

            iterator.next() // Skip A
            val b = iterator.next() // Get B
            b.get().street shouldBe "B"

            iterator.remove() // Removes B

            mutator.get() shouldBe listOf(
                Address("A", "C", "Z"),
                Address("C", "C", "Z")
            )

            iterator.nextIndex() shouldBe 1 // Next element to return is C (index 1 now)
        }

        "listIterator should support set" {
            val list = listOf(
                Address("A", "C", "Z"),
                Address("B", "C", "Z")
            )

            val mutator = list.mutator()
            val iterator = mutator.listIterator()

            val a = iterator.next()
            a.street += "!" // Mutating it also works, but let's test set

            // Setting a completely new element through the iterator
            val newItemMutator = mutator.getChildMutator(Address("New A", "C", "Z"))
            iterator.set(newItemMutator)

            mutator.get() shouldBe listOf(Address("New A", "C", "Z"), Address("B", "C", "Z"))
        }

        "listIterator should support add" {
            val list = listOf(
                Address("A", "C", "Z"),
                Address("B", "C", "Z")
            )

            val mutator = list.mutator()
            val iterator = mutator.listIterator()

            iterator.next() // Skip A

            val newItemMutator = mutator.getChildMutator(
                Address("Middle", "C", "Z")
            )

            iterator.add(newItemMutator)

            mutator.get() shouldBe listOf(
                Address("A", "C", "Z"),
                Address("Middle", "C", "Z"),
                Address("B", "C", "Z")
            )

            iterator.nextIndex() shouldBe 2 // Because we added one element before B
        }

        "iterator should not crash when list is structurally modified during iteration" {
            val list = listOf(
                Address("A", "City A", "Zip A"),
                Address("B", "City B", "Zip B"),
                Address("C", "City C", "Zip C")
            )

            val mutator = list.mutator()

            // We grab mutators from the iterator
            val iter = mutator.iterator()
            val aMutator = iter.next()
            val bMutator = iter.next()

            // Now, we structurally modify the list by removing the first element.
            // This shifts 'B' and 'C' down in index.
            mutator.removeAt(0)

            // The list is now: [B, C]

            // We modify the B element through the mutator we grabbed earlier.
            // If the old Iterator implementation was used, it would try to setAt(index=1),
            // overwriting 'C' instead of updating 'B' (which is now at index 0),
            // or crash if it was out of bounds!
            bMutator.street += "-changed"

            mutator.get() shouldBe listOf(
                Address("B-changed", "City B", "Zip B"),
                Address("C", "City C", "Zip C")
            )

            // Mutating 'A' should be ignored safely since it was removed from the list.
            aMutator.street += "-changed"

            mutator.get() shouldBe listOf(
                Address("B-changed", "City B", "Zip B"),
                Address("C", "City C", "Zip C")
            )
        }
    }
}
