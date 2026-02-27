package de.peekandpoke.mutator

import de.peekandpoke.mutator.domain.Address
import de.peekandpoke.mutator.domain.mutator
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MapMutatorNotificationsSpec : StringSpec({

    "put() should notify when element is new" {
        val map = mapOf("1" to Address("A", "C", "Z"))
        val mutator = map.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        mutator["2"] = mutator.getChildMutator(Address("B", "C", "Z"))

        notified shouldBe 1
    }

    "put() should notify when element changes" {
        val map = mapOf("1" to Address("A", "C", "Z"))
        val mutator = map.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        mutator["1"] = mutator.getChildMutator(Address("B", "C", "Z"))

        notified shouldBe 1
    }

    "put() should NOT notify when element is identical" {
        val a = Address("A", "C", "Z")
        val map = mapOf("1" to a)
        val mutator = map.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        mutator["1"] = mutator.getChildMutator(a)

        notified shouldBe 0
    }

    "remove() existing key should notify" {
        val map = mapOf("1" to Address("A", "C", "Z"))
        val mutator = map.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        mutator.remove("1")

        notified shouldBe 1
    }

    "remove() non-existing key should NOT notify" {
        val map = mapOf("1" to Address("A", "C", "Z"))
        val mutator = map.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        mutator.remove("2")

        notified shouldBe 0
    }

    "clear() on non-empty map should notify" {
        val map = mapOf("1" to Address("A", "C", "Z"))
        val mutator = map.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        mutator.clear()

        notified shouldBe 1
    }

    "clear() on empty map should NOT notify" {
        val map = emptyMap<String, Address>()
        val mutator = map.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        mutator.clear()

        notified shouldBe 0
    }

    "modifying a child mutator from get() should notify" {
        val map = mapOf("1" to Address("A", "C", "Z"))
        val mutator = map.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        mutator["1"]?.modifyValue { it.copy(street = "B") }

        notified shouldBe 1
    }

    "modifying a child mutator from entries should notify" {
        val map = mapOf("1" to Address("A", "C", "Z"))
        val mutator = map.mutator()
        var notified = 0
        mutator.onChange { notified++ }

        mutator.entries.first().value.modifyValue { it.copy(street = "B") }

        notified shouldBe 1
    }
})
