package io.peekandpoke.mutator

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.mutator.domain.SealedInterface
import io.peekandpoke.mutator.domain.filterMutatorsOf
import io.peekandpoke.mutator.domain.mutator

class ListMutatorHelpersSpec : StringSpec({

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

    "filterMutatorsOf should return only matching elements" {
        val list = listOf(
            SealedInterface.One("2"),
            SealedInterface.Two(4)
        )

        val mutator = list.mutator()

        val filtered = mutator.filterMutatorsOf<SealedInterface.One>()

        filtered.size shouldBe 1
    }
})
