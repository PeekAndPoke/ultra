package de.peekandpoke.mutator

import de.peekandpoke.mutator.domain.Address
import de.peekandpoke.mutator.domain.Person
import de.peekandpoke.mutator.domain.address
import de.peekandpoke.mutator.domain.mutate
import de.peekandpoke.mutator.domain.mutator
import de.peekandpoke.mutator.domain.street
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class DataClassMutatorSpec : StringSpec() {

    init {
        "Empty mutation must return the exact same object" {
            val subject = Address(street = "street", city = "city", zip = "zip")

            val result = subject.mutate { }

            result shouldBeSameInstanceAs subject
        }

        "Non empty mutation but setting the same value must return the exact same object" {
            val subject = Address(street = "street", city = "city", zip = "zip")

            val result = subject.mutate {
                street = "street"
            }

            result shouldBeSameInstanceAs subject
        }

        "Mutation that first sets the value and then resets it must return the exact same object" {
            val subject = Address(street = "street", city = "city", zip = "zip")

            val result = subject.mutate {
                street = "newStreet"
                street = "street"
            }

            result shouldBeSameInstanceAs subject
        }

        "Using mutator() and setting the same value must return the exact same object" {
            val subject = Address(street = "street", city = "city", zip = "zip")

            val mutator = subject.mutator()

            mutator.street = "street"

            mutator.isModified() shouldBe false
            mutator() shouldBeSameInstanceAs subject
            mutator.get() shouldBeSameInstanceAs subject
        }

        "Using mutator() and setting a value and then resetting it must return the exact same object" {
            val subject = Address(street = "street", city = "city", zip = "zip")

            val mutator = subject.mutator()

            mutator.street = "newStreet"
            mutator.street = "street"

            mutator.isModified() shouldBe false
            mutator() shouldBeSameInstanceAs subject
            mutator.get() shouldBeSameInstanceAs subject
        }

        "Mutating a field that is a mutator itself must return the exact same object when nothing was changed" {

            val subject = Person(
                firstName = "John",
                lastName = "Doe",
                age = 42,
                address = Address(street = "street", city = "city", zip = "zip"),
            )

            val mutator = subject.mutator()

            mutator {
                address {
                    // Set it
                    street = "newStreet"
                }
            }

            mutator.isModified() shouldBe true

            mutator {
                address {
                    // Then reset it
                    street = "street"
                }
            }

            mutator.isModified() shouldBe false

            mutator() shouldBeSameInstanceAs subject
        }
    }
}
