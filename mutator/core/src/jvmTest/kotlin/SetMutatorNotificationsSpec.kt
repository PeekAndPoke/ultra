package de.peekandpoke.mutator

import de.peekandpoke.mutator.domain.Address
import de.peekandpoke.mutator.domain.mutator
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SetMutatorNotificationsSpec : StringSpec({

    "add() should notify when element is new" {
        val set = setOf(Address("A", "C", "Z"))
        val mutator = set.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        mutator.add(Address("B", "C", "Z"))

        notified shouldBe 1
    }

    "add() should NOT notify when element is already in the set" {
        val a = Address("A", "C", "Z")
        val set = setOf(a)
        val mutator = set.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        mutator.add(a)

        notified shouldBe 0
    }

    "remove() existing element should notify" {
        val b = Address("B", "C", "Z")
        val set = setOf(Address("A", "C", "Z"), b)
        val mutator = set.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        mutator.remove(mutator.getChildMutator(b))

        notified shouldBe 1
    }

    "remove() non-existing element should NOT notify" {
        val set = setOf(Address("A", "C", "Z"))
        val mutator = set.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        val b = Address("B", "C", "Z")
        mutator.remove(mutator.getChildMutator(b))

        notified shouldBe 0
    }

    "clear() on non-empty set should notify" {
        val set = setOf(Address("A", "C", "Z"))
        val mutator = set.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        mutator.clear()

        notified shouldBe 1
    }

    "clear() on empty set should NOT notify" {
        val set = emptySet<Address>()
        val mutator = set.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        mutator.clear()

        notified shouldBe 0
    }

    "removeAll() modifying the set should notify" {
        val a = Address("A", "C", "Z")
        val b = Address("B", "C", "Z")
        val set = setOf(a, b)
        val mutator = set.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        mutator.removeAll(listOf(mutator.getChildMutator(a)))

        notified shouldBe 1
    }

    "removeAll() NOT modifying the set should NOT notify" {
        val set = setOf(Address("A", "C", "Z"))
        val mutator = set.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        val b = Address("B", "C", "Z")
        mutator.removeAll(listOf(mutator.getChildMutator(b)))

        notified shouldBe 0
    }

    "retainAll() modifying the set should notify" {
        val a = Address("A", "C", "Z")
        val b = Address("B", "C", "Z")
        val set = setOf(a, b)
        val mutator = set.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        mutator.retainAll(listOf(mutator.getChildMutator(a)))

        notified shouldBe 1
    }

    "retainAll() NOT modifying the set should NOT notify" {
        val a = Address("A", "C", "Z")
        val set = setOf(a)
        val mutator = set.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        mutator.retainAll(listOf(mutator.getChildMutator(a)))

        notified shouldBe 0
    }

    "addAll() adding elements should notify" {
        val set = setOf(Address("A", "C", "Z"))
        val mutator = set.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        val b = Address("B", "C", "Z")
        mutator.addAll(listOf(mutator.getChildMutator(b)))

        notified shouldBe 1
    }

    "addAll() adding elements already present should NOT notify" {
        val a = Address("A", "C", "Z")
        val set = setOf(a)
        val mutator = set.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        mutator.addAll(listOf(mutator.getChildMutator(a)))

        notified shouldBe 0
    }

    "addAll() empty collection should NOT notify" {
        val set = setOf(Address("A", "C", "Z"))
        val mutator = set.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        mutator.addAll(emptyList())

        notified shouldBe 0
    }
})
