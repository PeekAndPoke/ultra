package de.peekandpoke.ultra.mutator.e2e

import io.kotlintest.DisplayName
import io.kotlintest.assertSoftly
import io.kotlintest.matchers.withClue
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.StringSpec

@DisplayName("E2E - SetMutationsSpec")
class SetMutationsSpec : StringSpec({

    "Mutating a set ... using size and isEmpty" {

        val source = SetOfAddresses(
            addresses = setOf()
        )

        source.mutate {

            assertSoftly {
                addresses.size shouldBe 0
                addresses.isEmpty() shouldBe true

                addresses.add(
                    Address("Berlin", "10115"),
                    Address("Leipzig", "04109")
                )

                addresses.size shouldBe 2
                addresses.isEmpty() shouldBe false
            }
        }
    }

    "Mutating all objects in a set via forEach" {

        val source = SetOfAddresses(
            addresses = setOf(
                Address("Berlin", "10115"),
                Address("Leipzig", "04109")
            )
        )

        val result = source.mutate {
            addresses.forEach {
                it.city += "-x"
            }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("Result must be modified properly") {

                result.addresses shouldBe setOf(
                    Address("Berlin-x", "10115"),
                    Address("Leipzig-x", "04109")
                )
            }
        }
    }

    "Mutating some objects in a set via filter and forEach" {

        val source = SetOfAddresses(
            addresses = setOf(
                Address("Berlin", "10115"),
                Address("Leipzig", "04109")
            )
        )

        val result = source.mutate {
            addresses
                .filter { it.city == "Berlin" }
                .forEach {
                    it.city += " x"
                }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe setOf(
                    Address("Berlin x", "10115"),
                    Address("Leipzig", "04109")
                )
            }
        }
    }

    "Mutating an object by replacing a whole set" {

        val source = SetOfAddresses(
            addresses = setOf(
                Address("Berlin", "10115"),
                Address("Leipzig", "04109")
            )
        )

        val result = source.mutate {
            addresses += setOf(
                Address("Chemnitz", "09111")
            )
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe setOf(
                    Address("Chemnitz", "09111")
                )
            }
        }
    }

    "Mutating a set by clearing it via clear" {

        val source = SetOfAddresses(
            addresses = setOf(
                Address("Berlin", "10115"),
                Address("Leipzig", "04109")
            )
        )

        val result = source.mutate {
            addresses.clear()
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe setOf()
            }
        }
    }

    "Mutating a set by removing elements via filter" {

        val source = SetOfAddresses(
            addresses = setOf(
                Address("Berlin", "10115"),
                Address("Leipzig", "04109")
            )
        )

        val result = source.mutate {
            addresses += addresses.filter { it.city == "Leipzig" }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe setOf(
                    Address("Leipzig", "04109")
                )
            }
        }
    }

    "Mutating a set by adding elements via add" {

        val source = SetOfAddresses(
            addresses = setOf(
                Address("Berlin", "10115"),
                Address("Leipzig", "04109")
            )
        )

        val result = source.mutate {
            addresses.add(
                Address("Chemnitz", "09111"),
                Address("Bonn", "53111")
            )
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe setOf(
                    Address("Berlin", "10115"),
                    Address("Leipzig", "04109"),
                    Address("Chemnitz", "09111"),
                    Address("Bonn", "53111")
                )
            }
        }
    }

    "Mutating a set by removing elements via add remove" {

        val source = SetOfAddresses(
            addresses = setOf(
                Address("Berlin", "10115"),
                Address("Leipzig", "04109"),
                Address("Chemnitz", "09111")
            )
        )

        val result = source.mutate {
            addresses.remove(
                Address("Leipzig", "04109"),
                Address("Chemnitz", "09111")
            )
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe setOf(
                    Address("Berlin", "10115")
                )
            }
        }
    }

    "Mutating a set by removing elements via removeWhere" {

        val source = SetOfAddresses(
            addresses = setOf(
                Address("Berlin", "10115"),
                Address("Leipzig", "04109")
            )
        )

        val result = source.mutate {
            addresses.removeWhere { city == "Leipzig" }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe setOf(
                    Address("Berlin", "10115")
                )
            }
        }
    }

    "Mutating a set by removing elements via retainWhere" {

        val source = SetOfAddresses(
            addresses = setOf(
                Address("Berlin", "10115"),
                Address("Leipzig", "04109"),
                Address("Bonn", "53111")
            )
        )

        val result = source.mutate {
            addresses.retainWhere { city.startsWith("B") }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe setOf(
                    Address("Berlin", "10115"),
                    Address("Bonn", "53111")
                )
            }
        }
    }

})
