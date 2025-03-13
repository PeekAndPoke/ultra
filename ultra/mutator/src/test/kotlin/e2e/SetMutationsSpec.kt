package de.peekandpoke.ultra.mutator.e2e

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.DisplayName
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs

@DisplayName("E2E - SetMutationsSpec")
class SetMutationsSpec : StringSpec({

    "Mutating a set ... using size and isEmpty" {

        val source = SetOfAddresses(
            addresses = setOf()
        )

        val address1 = Address("Berlin", "10115")
        val address2 = Address("Leipzig", "04109")

        source.mutate {

            assertSoftly {
                addresses.size shouldBe 0
                addresses.isEmpty() shouldBe true

                addresses.add(
                    address1,
                    address2
                )

                addresses.size shouldBe 2
                addresses.isEmpty() shouldBe false

                addresses.add(
                    address1
                )

                addresses.size shouldBe 2
                addresses.isEmpty() shouldBe false

                addresses.remove(address1)

                addresses.size shouldBe 1
                addresses.isEmpty() shouldBe false

                addresses.remove(address1)

                addresses.size shouldBe 1
                addresses.isEmpty() shouldBe false

                addresses.remove(address2)

                addresses.size shouldBe 0
                addresses.isEmpty() shouldBe true
            }
        }
    }

    "Mutating all objects in a set via forEach" {

        val data = setOf(
            Address("Berlin", "10115"),
            Address("Leipzig", "04109")
        )
        val dataCopy = data.toSet()

        val source = SetOfAddresses(addresses = data)

        val result = source.mutate {
            addresses.forEach {
                it.city += "-x"
            }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.addresses shouldBe dataCopy
                source.addresses shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {

                val expected = setOf(
                    Address("Berlin-x", "10115"),
                    Address("Leipzig-x", "04109")
                )

                println(result.addresses::class)
                println(result.addresses)
                println(expected::class)
                println(expected)

                result.addresses shouldBe expected
            }
        }
    }

    "Mutating some objects in a set via filter and forEach" {

        val data = setOf(
            Address("Berlin", "10115"),
            Address("Leipzig", "04109")
        )
        val dataCopy = data.toSet()

        val source = SetOfAddresses(addresses = data)

        val result = source.mutate {
            addresses
                .filter { it.city == "Berlin" }
                .forEach {
                    it.city += " x"
                }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.addresses shouldBe dataCopy
                source.addresses shouldBeSameInstanceAs data
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

        val data = setOf(
            Address("Berlin", "10115"),
            Address("Leipzig", "04109")
        )
        val dataCopy = data.toSet()

        val source = SetOfAddresses(addresses = data)

        val result = source.mutate {
            addresses += setOf(
                Address("Chemnitz", "09111")
            )
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.addresses shouldBe dataCopy
                source.addresses shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe setOf(
                    Address("Chemnitz", "09111")
                )
            }
        }
    }

    "Mutating a set by clearing it via clear" {

        val data = setOf(
            Address("Berlin", "10115"),
            Address("Leipzig", "04109")
        )
        val dataCopy = data.toSet()

        val source = SetOfAddresses(addresses = data)

        val result = source.mutate {
            addresses.clear()
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.addresses shouldBe dataCopy
                source.addresses shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe setOf()
            }
        }
    }

    "Mutating a set by removing elements via filter" {

        val data = setOf(
            Address("Berlin", "10115"),
            Address("Leipzig", "04109")
        )
        val dataCopy = data.toSet()

        val source = SetOfAddresses(addresses = data)

        val result = source.mutate {
            addresses += addresses.filter { it.city == "Leipzig" }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.addresses shouldBe dataCopy
                source.addresses shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe setOf(
                    Address("Leipzig", "04109")
                )
            }
        }
    }

    "Mutating a set by adding elements via add" {

        val data = setOf(
            Address("Berlin", "10115"),
            Address("Leipzig", "04109")
        )
        val dataCopy = data.toSet()

        val source = SetOfAddresses(addresses = data)

        val result = source.mutate {
            addresses.add(
                Address("Chemnitz", "09111"),
                Address("Bonn", "53111")
            )
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.addresses shouldBe dataCopy
                source.addresses shouldBeSameInstanceAs data
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

    "Mutating a set by removing elements by hash code via add remove" {

        val data = setOf(
            Address("Berlin", "10115"),
            Address("Leipzig", "04109"),
            Address("Chemnitz", "09111")
        )
        val dataCopy = data.toSet()

        val source = SetOfAddresses(addresses = data)

        val result = source.mutate {
            addresses.remove(
                Address("Leipzig", "04109"),
                Address("Chemnitz", "09111")
            )
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.addresses shouldBe dataCopy
                source.addresses shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe setOf(
                    Address("Berlin", "10115")
                )
            }
        }
    }

    "Mutating a set by removing elements by instance code via add remove" {

        val address2 = Address("Leipzig", "04109")
        val address3 = Address("Chemnitz", "09111")

        val data = setOf(
            Address("Berlin", "10115"),
            address2,
            address3
        )
        val dataCopy = data.toSet()

        val source = SetOfAddresses(addresses = data)

        val result = source.mutate {
            addresses.remove(
                address2,
                address3
            )
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.addresses shouldBe dataCopy
                source.addresses shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe setOf(
                    Address("Berlin", "10115")
                )
            }
        }
    }

    "Mutating a set by removing elements via removeWhere" {

        val data = setOf(
            Address("Berlin", "10115"),
            Address("Leipzig", "04109")
        )
        val dataCopy = data.toSet()

        val source = SetOfAddresses(addresses = data)

        val result = source.mutate {
            addresses.removeWhere { city == "Leipzig" }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.addresses shouldBe dataCopy
                source.addresses shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe setOf(
                    Address("Berlin", "10115")
                )
            }
        }
    }

    "Mutating a set by removing elements via retainWhere" {

        val data = setOf(
            Address("Berlin", "10115"),
            Address("Leipzig", "04109"),
            Address("Bonn", "53111")
        )
        val dataCopy = data.toSet()

        val source = SetOfAddresses(addresses = data)

        val result = source.mutate {
            addresses.retainWhere { city.startsWith("B") }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.addresses shouldBe dataCopy
                source.addresses shouldBeSameInstanceAs data
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
