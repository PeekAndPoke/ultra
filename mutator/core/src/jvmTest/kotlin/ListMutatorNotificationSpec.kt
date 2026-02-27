package de.peekandpoke.mutator

import de.peekandpoke.mutator.domain.Address
import de.peekandpoke.mutator.domain.mutator
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ListMutatorNotificationsSpec : StringSpec({

    "add() should notify" {
        val list = listOf(Address("A", "C", "Z"))
        val mutator = list.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        mutator.add(Address("B", "C", "Z"))

        notified shouldBe 1
    }

    "remove() existing element should notify" {
        val b = Address("B", "C", "Z")
        val list = listOf(Address("A", "C", "Z"), b)
        val mutator = list.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        mutator.remove(mutator.getChildMutator(b))

        notified shouldBe 1
    }

    "remove() non-existing element should NOT notify" {
        val list = listOf(Address("A", "C", "Z"))
        val mutator = list.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        val b = Address("B", "C", "Z")
        mutator.remove(mutator.getChildMutator(b))

        notified shouldBe 0
    }

    "clear() on non-empty list should notify" {
        val list = listOf(Address("A", "C", "Z"))
        val mutator = list.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        mutator.clear()

        notified shouldBe 1
    }

    "clear() on empty list should NOT notify" {
        val list = emptyList<Address>()
        val mutator = list.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        mutator.clear()

        notified shouldBe 0
    }

    "removeAll() modifying the list should notify" {
        val a = Address("A", "C", "Z")
        val b = Address("B", "C", "Z")
        val list = listOf(a, b)
        val mutator = list.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        mutator.removeAll(listOf(mutator.getChildMutator(a)))

        notified shouldBe 1
    }

    "removeAll() NOT modifying the list should NOT notify" {
        val list = listOf(Address("A", "C", "Z"))
        val mutator = list.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        val b = Address("B", "C", "Z")
        mutator.removeAll(listOf(mutator.getChildMutator(b)))

        notified shouldBe 0
    }

    "retainAll() modifying the list should notify" {
        val a = Address("A", "C", "Z")
        val b = Address("B", "C", "Z")
        val list = listOf(a, b)
        val mutator = list.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        mutator.retainAll(listOf(mutator.getChildMutator(a)))

        notified shouldBe 1
    }

    "retainAll() NOT modifying the list should NOT notify" {
        val a = Address("A", "C", "Z")
        val list = listOf(a)
        val mutator = list.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        mutator.retainAll(listOf(mutator.getChildMutator(a)))

        notified shouldBe 0
    }

    "addAll() adding elements should notify" {
        val list = listOf(Address("A", "C", "Z"))
        val mutator = list.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        val b = Address("B", "C", "Z")
        mutator.addAll(listOf(mutator.getChildMutator(b)))

        notified shouldBe 1
    }

    "addAll() empty collection should NOT notify" {
        val list = listOf(Address("A", "C", "Z"))
        val mutator = list.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        mutator.addAll(emptyList())

        notified shouldBe 0
    }

    "set() replacing with the exact same element should NOT notify" {
        val a = Address("A", "C", "Z")
        val list = listOf(a)
        val mutator = list.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        mutator[0] = mutator.getChildMutator(a)

        notified shouldBe 0
    }
})
