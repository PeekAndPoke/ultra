package de.peekandpoke.mutator

import de.peekandpoke.mutator.domain.Address
import de.peekandpoke.mutator.domain.SealedInterface
import de.peekandpoke.mutator.domain.filterMutatorsOf
import de.peekandpoke.mutator.domain.mutator
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ListMutatorHelpersSpec : StringSpec({

    "filterMutatorsOf should return only mutators matching the given type" {
        val list = listOf(
            Address("Street 1", "City 1", "Zip 1"),
            Address("Street 2", "City 2", "Zip 2")
        )

        val mutator = list.mutator()

        val filtered = mutator.filterMutatorsOf<Address>()

        filtered.size shouldBe 2
        filtered[0].get().shouldBeInstanceOf<Address>()
        filtered[1].get().shouldBeInstanceOf<Address>()
        filtered[0].get().street shouldBe "Street 1"
        filtered[1].get().street shouldBe "Street 2"
    }

    "filterMutatorsOf should work with sealed interfaces" {
        val list = listOf(
            SealedInterface.One("Value 1"),
            SealedInterface.Two(2),
            SealedInterface.One("Value 3"),
            SealedInterface.Two(4)
        )

        val mutator = list.mutator()

        val typeOneMutators = mutator.filterMutatorsOf<SealedInterface.One>()

        typeOneMutators.size shouldBe 2
        typeOneMutators[0].get().value shouldBe "Value 1"
        typeOneMutators[1].get().value shouldBe "Value 3"
    }

    "filterMutatorsOf allows direct mutation on the filtered mutators" {
        val list = listOf(
            SealedInterface.One("Value 1"),
            SealedInterface.Two(2),
            SealedInterface.One("Value 3"),
            SealedInterface.Two(4)
        )

        val mutator = list.mutator()

        mutator.filterMutatorsOf<SealedInterface.One>().forEach { item ->
            item.modifyValue { it.copy(value = it.value + "-changed") }
        }

        mutator.get() shouldBe listOf(
            SealedInterface.One("Value 1-changed"),
            SealedInterface.Two(2),
            SealedInterface.One("Value 3-changed"),
            SealedInterface.Two(4)
        )
    }

    "filterMutatorsOf should return empty list when no elements match" {
        val list = listOf(
            SealedInterface.One("2"),
            SealedInterface.Two(4)
        )

        val mutator = list.mutator()

        val filtered = mutator.filterMutatorsOf<SealedInterface.One>()

        filtered.size shouldBe 1
    }
})
