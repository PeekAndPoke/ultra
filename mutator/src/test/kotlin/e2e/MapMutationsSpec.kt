package de.peekandpoke.ultra.mutator.e2e

import io.kotlintest.DisplayName
import io.kotlintest.assertSoftly
import io.kotlintest.matchers.withClue
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.StringSpec

@DisplayName("E2E - MapMutationsSpec")
class MapMutationsSpec : StringSpec({

    "Mutating but not changing any value is treated like no mutation" {

        val address1 = Address("Berlin", "10115")

        val source = MapOfAddresses(
            addresses = mapOf(
                "B" to address1,
                "L" to Address("Leipzig", "04109")
            )
        )

        val result = source.mutate {
            // setting the same object must not trigger mutation
            addresses["B"] = address1

            // iteration must no trigger mutation on its own
            addresses.forEach { (_, address) ->
                // setting the same value on a child object must not trigger mutation
                address.city = address.city
            }
        }

        assertSoftly {

            withClue("When all mutations are not changing any value, the source object must be returned") {
                (source === result) shouldBe true
            }
        }
    }

    "Mutating a list ... using size and isEmpty" {

        val source = MapOfAddresses(
            addresses = mapOf()
        )

        source.mutate {

            assertSoftly {

                addresses.size shouldBe 0
                addresses.isEmpty() shouldBe true

                addresses.put(
                    "B" to Address("Berlin", "10115"),
                    "L" to Address("Leipzig", "04109")
                )

                addresses.size shouldBe 2
                addresses.isEmpty() shouldBe false
            }
        }
    }

    "Mutating properties of elements in a map" {

        val source = MapOfAddresses(
            addresses = mapOf(
                "B" to Address("Berlin", "10115"),
                "L" to Address("Leipzig", "04109")
            )
        )

        val result = source.mutate {
            addresses["B"]?.apply {
                city = city.toUpperCase()
            }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("First list element must be modified") {
                source.addresses["B"] shouldNotBe result.addresses["B"]
            }

            withClue("Second list element must stay the same") {
                source.addresses["L"] shouldBe result.addresses["L"]
            }

            withClue("Result must be modified properly") {

                result.addresses shouldBe mapOf(
                    "B" to Address("BERLIN", "10115"),
                    "L" to Address("Leipzig", "04109")
                )
            }
        }
    }

    "Mutating a map by overwriting an element via set" {

        val source = MapOfAddresses(
            addresses = mapOf(
                "B" to Address("Berlin", "10115"),
                "L" to Address("Leipzig", "04109")
            )
        )

        val result = source.mutate {
            addresses["B"] = Address("Bonn", "53111")
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("First list element must be modified") {
                source.addresses["B"] shouldNotBe result.addresses["B"]
            }

            withClue("Second list element must stay the same") {
                source.addresses["L"] shouldBe result.addresses["L"]
            }

            withClue("Result must be modified properly") {

                result.addresses shouldBe mapOf(
                    "B" to Address("Bonn", "53111"),
                    "L" to Address("Leipzig", "04109")
                )
            }
        }
    }

    "Mutating a map by adding an element via set" {

        val source = MapOfAddresses(
            addresses = mapOf(
                "B" to Address("Berlin", "10115"),
                "L" to Address("Leipzig", "04109")
            )
        )

        val result = source.mutate {
            addresses["C"] = Address("Chemnitz", "09111")
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("Source map elements must stay the same") {
                source.addresses shouldBe mapOf(
                    "B" to Address("Berlin", "10115"),
                    "L" to Address("Leipzig", "04109")
                )
            }

            withClue("Result must be modified properly") {

                result.addresses shouldBe mapOf(
                    "B" to Address("Berlin", "10115"),
                    "L" to Address("Leipzig", "04109"),
                    "C" to Address("Chemnitz", "09111")
                )
            }
        }
    }

    "Mutating a map by adding elements via put" {

        val source = MapOfAddresses(
            addresses = mapOf(
                "B" to Address("Berlin", "10115"),
                "L" to Address("Leipzig", "04109")
            )
        )

        val result = source.mutate {
            addresses.put(
                "C" to Address("Chemnitz", "09111"),
                "O" to Address("Bonn", "53111")
            )
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("Source map elements must stay the same") {
                source.addresses shouldBe mapOf(
                    "B" to Address("Berlin", "10115"),
                    "L" to Address("Leipzig", "04109")
                )
            }

            withClue("Result must be modified properly") {

                result.addresses shouldBe mapOf(
                    "B" to Address("Berlin", "10115"),
                    "L" to Address("Leipzig", "04109"),
                    "C" to Address("Chemnitz", "09111"),
                    "O" to Address("Bonn", "53111")
                )
            }
        }
    }

    "Mutating all elements in a map via forEach" {

        val source = MapOfAddresses(
            addresses = mapOf(
                "B" to Address("Berlin", "10115"),
                "L" to Address("Leipzig", "04109")
            )
        )

        val result = source.mutate {
            addresses.forEach { (_, address) ->
                address.city += " x"
            }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe mapOf(
                    "B" to Address("Berlin x", "10115"),
                    "L" to Address("Leipzig x", "04109")
                )
            }
        }
    }

    "Mutating all elements in a map via multiple forEach loops" {

        val source = MapOfAddresses(
            addresses = mapOf(
                "B" to Address("Berlin", "10115"),
                "L" to Address("Leipzig", "04109")
            )
        )

        val result = source.mutate {
            addresses.forEach { (_, address) ->
                address.city += " x"
            }

            addresses.forEach { (_, address) ->
                address.zip += " y"
            }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe mapOf(
                    "B" to Address("Berlin x", "10115 y"),
                    "L" to Address("Leipzig x", "04109 y")
                )
            }
        }
    }

    "Mutating a map by clearing it via clear" {

        val source = MapOfAddresses(
            addresses = mapOf(
                "B" to Address("Berlin", "10115"),
                "L" to Address("Leipzig", "04109")
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
                result.addresses shouldBe mapOf()
            }
        }
    }

    "Mutating a map by removing elements via filter" {

        val source = MapOfAddresses(
            addresses = mapOf(
                "B" to Address("Berlin", "10115"),
                "L" to Address("Leipzig", "04109")
            )
        )

        val result = source.mutate {
            addresses += addresses.filter { it.value.city == "Leipzig" }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe mapOf(
                    "L" to Address("Leipzig", "04109")
                )
            }
        }
    }

    "Mutating some elements in a map via filter and forEach" {

        val source = MapOfAddresses(
            addresses = mapOf(
                "B" to Address("Berlin", "10115"),
                "L" to Address("Leipzig", "04109"),
                "C" to Address("Chemnitz", "09111"),
                "O" to Address("Bonn", "53111")
            )
        )

        val result = source.mutate {
            addresses
                .filter { (k, v) -> k == "L" || v.city.startsWith("B") }
                .forEach { it.value.city += " x" }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe mapOf(
                    "B" to Address("Berlin x", "10115"),
                    "L" to Address("Leipzig x", "04109"),
                    "C" to Address("Chemnitz", "09111"), // This entry does not match the filter
                    "O" to Address("Bonn x", "53111")
                )
            }
        }
    }

    "Mutating a map by replacing the whole map" {

        val source = MapOfAddresses(
            addresses = mapOf(
                "B" to Address("Berlin", "10115"),
                "L" to Address("Leipzig", "04109")
            )
        )

        val result = source.mutate {
            addresses += mapOf(
                "C" to Address("Chemnitz", "09111")
            )
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe mapOf(
                    "C" to Address("Chemnitz", "09111")
                )
            }
        }
    }


    "Mutating a map by removing elements via removeWhere" {

        val source = MapOfAddresses(
            addresses = mapOf(
                "B" to Address("Berlin", "10115"),
                "L" to Address("Leipzig", "04109"),
                "C" to Address("Chemnitz", "09111")
            )
        )

        val result = source.mutate {
            addresses.removeWhere { it.value.city == "Leipzig" || it.key == "C" }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe mapOf(
                    "B" to Address("Berlin", "10115")
                )
            }
        }
    }

    "Mutating a map by removing elements via retainWhere" {

        val source = MapOfAddresses(
            addresses = mapOf(
                "B" to Address("Berlin", "10115"),
                "L" to Address("Leipzig", "04109"),
                "C" to Address("Chemnitz", "09111")
            )
        )

        val result = source.mutate {
            addresses.retainWhere { (k, v) -> v.city == "Leipzig" || k == "C" }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe mapOf(
                    "L" to Address("Leipzig", "04109"),
                    "C" to Address("Chemnitz", "09111")
                )
            }
        }
    }

    "Mutating a map by removing elements via remove" {

        val source = MapOfAddresses(
            addresses = mapOf(
                "B" to Address("Berlin", "10115"),
                "L" to Address("Leipzig", "04109"),
                "C" to Address("Chemnitz", "09111")
            )
        )

        val result = source.mutate {
            addresses.remove("B", "C")
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.addresses shouldNotBe result.addresses
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe mapOf(
                    "L" to Address("Leipzig", "04109")
                )
            }
        }
    }
})
