package de.peekandpoke.mutator

import de.peekandpoke.mutator.domain.Address
import de.peekandpoke.mutator.domain.SealedInterface
import de.peekandpoke.mutator.domain.mutate
import de.peekandpoke.mutator.domain.mutator
import de.peekandpoke.mutator.domain.street
import de.peekandpoke.mutator.domain.value
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SetMutatorSpec : StringSpec() {

    init {
        "should mutate set" {
            val set = setOf(
                Address("Street 1", "City 1", "Zip 1"),
                Address("Street 2", "City 2", "Zip 2")
            )

            val mutated = set.mutate {
                forEach {
                    it.street += " Mutated"
                }
            }

            mutated shouldBe setOf(
                Address("Street 1 Mutated", "City 1", "Zip 1"),
                Address("Street 2 Mutated", "City 2", "Zip 2")
            )
        }

        "forEach should iterate over all mutators" {
            val set = setOf(
                Address("A", "City", "Zip"),
                Address("B", "City", "Zip")
            )

            var count = 0
            val mutated = set.mutate {
                forEach { mutator ->
                    count++
                    mutator.street += count.toString()
                }
            }

            count shouldBe 2
            mutated shouldBe setOf(
                Address("A1", "City", "Zip"),
                Address("B2", "City", "Zip")
            )
        }

        "forEachIndexed should iterate over all mutators with indices" {
            val set = setOf(
                Address("A", "City", "Zip"),
                Address("B", "City", "Zip")
            )

            val mutated = set.mutate {
                // Since Sets have no guaranteed iteration order unless it's LinkedHashSet (which setOf creates)
                // We just rely on the order preserved by Kotlin's setOf
                forEachIndexed { index, mutator ->
                    mutator.street += index.toString()
                }
            }

            mutated shouldBe setOf(
                Address("A0", "City", "Zip"),
                Address("B1", "City", "Zip")
            )
        }

        "map should transform mutators" {
            val set = setOf(
                Address("A", "City", "Zip"),
                Address("B", "City", "Zip")
            )

            val mutated = set.mutate {
                forEach { it.street += "!" }
            }

            mutated shouldBe setOf(
                Address("A!", "City", "Zip"),
                Address("B!", "City", "Zip")
            )
        }

        "mutator().forEach should iterate and mutate correctly" {
            val set = setOf(
                Address("A", "City", "Zip"),
                Address("B", "City", "Zip")
            )

            val mutator = set.mutator()

            mutator.forEach {
                it.street += "!"
            }

            mutator.get() shouldBe setOf(
                Address("A!", "City", "Zip"),
                Address("B!", "City", "Zip")
            )
        }

        "mutator().forEachIndexed should iterate and mutate correctly" {
            val set = setOf(
                Address("A", "City", "Zip"),
                Address("B", "City", "Zip")
            )

            val mutator = set.mutator()

            mutator.forEachIndexed { index, item ->
                item.street += index.toString()
            }

            mutator.get() shouldBe setOf(
                Address("A0", "City", "Zip"),
                Address("B1", "City", "Zip")
            )
        }

        "mutator().forEach should work with a set of sealed interfaces" {
            val set = setOf(
                SealedInterface.One("1"),
                SealedInterface.Two(2)
            )

            val mutator = set.mutator()

            mutator.forEach { item ->
                when (val v = item.get()) {
                    is SealedInterface.One -> item.cast(v) { value += "-changed" }
                    is SealedInterface.Two -> item.cast(v) { value += 10 }
                }
            }

            mutator.get() shouldBe setOf(
                SealedInterface.One("1-changed"),
                SealedInterface.Two(12)
            )
        }

        "mutator().forEachIndexed should work with a set of sealed interfaces" {
            val set = setOf(
                SealedInterface.One("1"),
                SealedInterface.Two(2)
            )

            val mutator = set.mutator()

            mutator.forEachIndexed { index, item ->
                when (val v = item.get()) {
                    is SealedInterface.One -> item.cast(v) { value += "-$index" }
                    is SealedInterface.Two -> item.cast(v) { value += index }
                }
            }

            mutator.get() shouldBe setOf(
                SealedInterface.One("1-0"),
                SealedInterface.Two(3)
            )
        }

        "iterator should support hasNext and next" {
            val set = setOf(
                Address("A", "C", "Z"),
                Address("B", "C", "Z")
            )

            val mutator = set.mutator()
            val iterator = mutator.iterator()

            iterator.hasNext() shouldBe true
            iterator.next().get().street shouldBe "A"

            iterator.hasNext() shouldBe true
            iterator.next().get().street shouldBe "B"

            iterator.hasNext() shouldBe false
        }

        "iterator should support remove" {
            val set = setOf(
                Address("A", "C", "Z"),
                Address("B", "C", "Z"),
                Address("C", "C", "Z")
            )

            val mutator = set.mutator()
            val iterator = mutator.iterator()

            iterator.next() // Skip A
            val b = iterator.next() // Get B
            b.get().street shouldBe "B"

            iterator.remove() // Removes B

            mutator.get() shouldBe setOf(
                Address("A", "C", "Z"),
                Address("C", "C", "Z")
            )
        }

        "iterator should not crash when set is structurally modified during iteration" {
            val set = setOf(
                Address("A", "City A", "Zip A"),
                Address("B", "City B", "Zip B"),
                Address("C", "City C", "Zip C")
            )

            val mutator = set.mutator()

            // We grab mutators from the iterator
            val iter = mutator.iterator()
            val aMutator = iter.next()
            val bMutator = iter.next()

            // Modify structurally by dropping A through another mutator reference
            mutator.remove(aMutator)

            // The set is now: [B, C]

            // Modifying B should work perfectly and re-insert it successfully
            bMutator.street += "-changed"

            // A was removed, its mutation may execute but won't be replaced in the set
            // because `replace(last, new)` won't find `last` in the set anymore.
            aMutator.street += "-changed"

            mutator.get() shouldBe setOf(
                Address("B-changed", "City B", "Zip B"),
                Address("C", "City C", "Zip C")
            )
        }

        "iterator should allow multiple modifications to the same element without duplicating" {
            val set = setOf(Address("A", "C", "Z"))
            val mutator = set.mutator()

            val iter = mutator.iterator()
            val aMutator = iter.next()

            // First modification
            aMutator.street += "-1"

            mutator.get() shouldBe setOf(Address("A-1", "C", "Z"))

            // Second modification on the same mutator instance
            aMutator.street += "-2"

            // It should cleanly replace A-1 with A-1-2.
            // If the bug existed, we would have [A-1, A-1-2] because it would try to remove A, which is already gone!
            mutator.get() shouldBe setOf(Address("A-1-2", "C", "Z"))
        }
    }
}
