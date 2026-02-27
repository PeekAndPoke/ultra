package de.peekandpoke.mutator

import de.peekandpoke.mutator.domain.Address
import de.peekandpoke.mutator.domain.mutate
import de.peekandpoke.mutator.domain.mutator
import de.peekandpoke.mutator.domain.street
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MapMutatorSpec : StringSpec({

    "should mutate map values" {
        val map = mapOf(
            "1" to Address("Street 1", "City 1", "Zip 1"),
            "2" to Address("Street 2", "City 2", "Zip 2")
        )

        val mutated = map.mutate {
            forEach { (_, mutator) ->
                mutator.street += " Mutated"
            }
        }

        mutated shouldBe mapOf(
            "1" to Address("Street 1 Mutated", "City 1", "Zip 1"),
            "2" to Address("Street 2 Mutated", "City 2", "Zip 2")
        )
    }

    "should allow adding new entries" {
        val map = mapOf("1" to Address("Street 1", "City 1", "Zip 1"))
        val mutator = map.mutator()

        val newItem = mutator.getChildMutator(Address("Street 2", "City 2", "Zip 2"))
        mutator.put("2", newItem)

        mutator.get() shouldBe mapOf(
            "1" to Address("Street 1", "City 1", "Zip 1"),
            "2" to Address("Street 2", "City 2", "Zip 2")
        )
    }

    "should allow removing entries" {
        val map = mapOf(
            "1" to Address("Street 1", "City 1", "Zip 1"),
            "2" to Address("Street 2", "City 2", "Zip 2")
        )

        val mutator = map.mutator()
        mutator.remove("1")

        mutator.get() shouldBe mapOf(
            "2" to Address("Street 2", "City 2", "Zip 2")
        )
    }

    "getting a value and mutating it updates the map" {
        val map = mapOf("1" to Address("Street 1", "City 1", "Zip 1"))
        val mutator = map.mutator()

        val itemMutator = mutator["1"]!!
        itemMutator.street += " Mutated"

        mutator.get() shouldBe mapOf(
            "1" to Address("Street 1 Mutated", "City 1", "Zip 1")
        )
    }
})
