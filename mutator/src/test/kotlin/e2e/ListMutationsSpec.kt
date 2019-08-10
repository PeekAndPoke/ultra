package de.peekandpoke.ultra.mutator.e2e

import io.kotlintest.DisplayName
import io.kotlintest.assertSoftly
import io.kotlintest.matchers.withClue
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.StringSpec

@DisplayName("E2E - ListMutationsSpec")
class ListMutationsSpec : StringSpec({

    "Mutating but not changing any value is treated like no mutation" {

        val address1 = Address("Berlin", "10115")

        val source = ListOfAddresses(
            addresses = listOf(
                address1,
                Address("Leipzig", "04109")
            )
        )

        val result = source.mutate {
            // setting the same object must not trigger mutation
            addresses[0] = address1

            // iteration must no trigger mutation on its own
            addresses.forEach {
                // setting the same value on a child object must not trigger mutation
                it.city = it.city
            }
        }

        assertSoftly {

            withClue("When all mutations are not changing any value, the source object must be returned") {
                (source === result) shouldBe true
            }
        }
    }

    "Mutating a list ... using size and isEmpty" {

        val source = ListOfAddresses(
            addresses = listOf()
        )

        source.mutate {

            assertSoftly {

                addresses.size shouldBe 0
                addresses.isEmpty() shouldBe true

                addresses.push(
                    Address("Berlin", "10115"),
                    Address("Leipzig", "04109")
                )

                addresses.size shouldBe 2
                addresses.isEmpty() shouldBe false
            }
        }
    }

    "Mutating properties of elements in a list" {

        val source = ListOfAddresses(
            addresses = listOf(
                Address("Berlin", "10115"),
                Address("Leipzig", "04109")
            )
        )

        val result = source.mutate {

            addresses[0].city = addresses[0].city.toUpperCase()
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("First list element must be modified") {
                source.addresses[0] shouldNotBe result.addresses[0]
            }

            withClue("Second list element must stay the same") {
                source.addresses[1] shouldBe result.addresses[1]
            }

            withClue("Result must be modified properly") {
                result.addresses[0] shouldBe Address("BERLIN", "10115")
                result.addresses[1] shouldBe Address("Leipzig", "04109")
            }
        }
    }

    "Mutating all elements in a list via forEach" {

        val source = ListOfAddresses(
            addresses = listOf(
                Address("Berlin", "10115"),
                Address("Leipzig", "04109")
            )
        )

        val result = source.mutate {

            addresses.forEach {
                it.city += " x"
            }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("Result must be modified properly") {
                result.addresses[0] shouldBe Address("Berlin x", "10115")
                result.addresses[1] shouldBe Address("Leipzig x", "04109")
            }
        }
    }

    "Mutating all elements in a list via multiple forEach loops" {

        val source = ListOfAddresses(
            addresses = listOf(
                Address("Berlin", "10115"),
                Address("Leipzig", "04109")
            )
        )

        val result = source.mutate {

            addresses.forEach {
                it.city += " x"
            }

            addresses.forEach {
                it.zip += " y"
            }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("Result must be modified properly") {
                result.addresses[0] shouldBe Address("Berlin x", "10115 y")
                result.addresses[1] shouldBe Address("Leipzig x", "04109 y")
            }
        }
    }

    "Mutating some elements in a list via filter and forEach" {

        val source = ListOfAddresses(
            addresses = listOf(
                Address("Berlin", "10115"),
                Address("Leipzig", "04109")
            )
        )

        val result = source.mutate {
            addresses
                .filter { it.city == "Berlin" }
                .forEach { it.city += " x" }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("Result must be modified properly") {
                result.addresses[0] shouldBe Address("Berlin x", "10115")
                result.addresses[1] shouldBe Address("Leipzig", "04109")
            }
        }
    }

    "Mutating a list by adding elements via push" {

        val source = ListOfAddresses(
            addresses = listOf(
                Address("Berlin", "10115"),
                Address("Leipzig", "04109")
            )
        )

        val result = source.mutate {
            addresses.push(
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
                result.addresses shouldBe listOf(
                    Address("Berlin", "10115"),
                    Address("Leipzig", "04109"),
                    Address("Chemnitz", "09111"),
                    Address("Bonn", "53111")
                )
            }
        }
    }

    "Mutating a list by removing elements via pop" {

        val source = ListOfAddresses(
            addresses = listOf(
                Address("Berlin", "10115"),
                Address("Leipzig", "04109")
            )
        )

        lateinit var popped: Address

        val result = source.mutate {
            popped = addresses.pop()!!
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("The last entry must be popped correctly") {
                popped shouldBe Address("Leipzig", "04109")
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe listOf(
                    Address("Berlin", "10115")
                )
            }
        }
    }

    "Mutating a list by adding elements via unshift" {

        val source = ListOfAddresses(
            addresses = listOf(
                Address("Berlin", "10115"),
                Address("Leipzig", "04109")
            )
        )

        val result = source.mutate {
            addresses.unshift(
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
                result.addresses shouldBe listOf(
                    Address("Chemnitz", "09111"),
                    Address("Bonn", "53111"),
                    Address("Berlin", "10115"),
                    Address("Leipzig", "04109")
                )
            }
        }
    }

    "Mutating a list by removing elements via shift" {

        val source = ListOfAddresses(
            addresses = listOf(
                Address("Berlin", "10115"),
                Address("Leipzig", "04109")
            )
        )

        lateinit var shifted: Address

        val result = source.mutate {
            shifted = addresses.shift()!!
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("The first entry must be shifted correctly") {
                shifted shouldBe Address("Berlin", "10115")
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe listOf(
                    Address("Leipzig", "04109")
                )
            }
        }
    }

    "Mutating a list by replacing a whole list" {

        val source = ListOfAddresses(
            addresses = listOf(
                Address("Berlin", "10115"),
                Address("Leipzig", "04109")
            )
        )

        val result = source.mutate {
            addresses += listOf(
                Address("Chemnitz", "09111")
            )
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe listOf(
                    Address("Chemnitz", "09111")
                )
            }
        }
    }

    "Mutating a list by clearing it via clear" {

        val source = ListOfAddresses(
            addresses = listOf(
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
                result.addresses shouldBe listOf()
            }
        }
    }

    "Mutating a list by removing elements via filter" {

        val source = ListOfAddresses(
            addresses = listOf(
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
                result.addresses shouldBe listOf(
                    Address("Leipzig", "04109")
                )
            }
        }
    }

    "Mutating a list by removing elements via removeWhere" {

        val source = ListOfAddresses(
            addresses = listOf(
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
                result.addresses shouldBe listOf(
                    Address("Berlin", "10115")
                )
            }
        }
    }

    "Mutating a list by removing elements via retainWhere" {

        val source = ListOfAddresses(
            addresses = listOf(
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
                result.addresses shouldBe listOf(
                    Address("Berlin", "10115"),
                    Address("Bonn", "53111")
                )
            }
        }
    }

    "Mutating a list by removing elements via remove" {

        val source = ListOfAddresses(
            addresses = listOf(
                Address("Berlin", "10115"),
                Address("Leipzig", "04109"),
                Address("Bonn", "53111")
            )
        )

        val result = source.mutate {
            addresses.remove(
                Address("Berlin", "10115"),
                Address("Bonn", "53111")
            )
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe listOf(
                    Address("Leipzig", "04109")
                )
            }
        }
    }

    "Mutating a list by removing elements via removeAt" {

        val source = ListOfAddresses(
            addresses = listOf(
                Address("Berlin", "10115"),
                Address("Leipzig", "04109"),
                Address("Bonn", "53111")
            )
        )

        val result = source.mutate {
            addresses.removeAt(1)
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe listOf(
                    Address("Berlin", "10115"),
                    Address("Bonn", "53111")
                )
            }
        }
    }
})
