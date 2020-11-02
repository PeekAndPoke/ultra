package de.peekandpoke.ultra.mutator.e2e

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.DisplayName
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs

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
                address.city = address.city + ""
            }
        }

        assertSoftly {

            withClue("When all mutations are not changing any value, the source object must be returned") {
                source shouldBeSameInstanceAs result
                source.addresses shouldBeSameInstanceAs result.addresses
                source.addresses["B"] shouldBeSameInstanceAs result.addresses["B"]
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

                addresses.put(
                    "B" to Address("Berlin", "10115")
                )

                addresses.size shouldBe 2
                addresses.isEmpty() shouldBe false

                addresses.remove("B")

                addresses.size shouldBe 1
                addresses.isEmpty() shouldBe false

                addresses.remove("B")

                addresses.size shouldBe 1
                addresses.isEmpty() shouldBe false

                addresses.remove("L")

                addresses.size shouldBe 0
                addresses.isEmpty() shouldBe true
            }
        }
    }

    "Mutating properties of elements in a map" {

        val data = mapOf(
            "B" to Address("Berlin", "10115"),
            "L" to Address("Leipzig", "04109")
        )
        val dataCopy = data.toMap()

        val source = MapOfAddresses(addresses = data)

        val result = source.mutate {
            addresses["B"]?.apply {
                city = city.toUpperCase()
            }
        }

        withClue("Source object must NOT be modified") {
            source shouldNotBeSameInstanceAs result
            source.addresses shouldBe dataCopy
            source.addresses shouldBeSameInstanceAs data
        }

        withClue("First list element must be modified") {
            source.addresses["B"] shouldNotBe result.addresses["B"]
        }

        withClue("Second list element must stay the same") {
            source.addresses["L"] shouldBe result.addresses["L"]
            source.addresses["L"] shouldBeSameInstanceAs result.addresses["L"]
        }

        withClue("Result must be modified properly") {

            result.addresses shouldBe mapOf(
                "B" to Address("BERLIN", "10115"),
                "L" to Address("Leipzig", "04109")
            )
        }
    }

    "Mutating a map by overwriting an element via set" {

        val data = mapOf(
            "B" to Address("Berlin", "10115"),
            "L" to Address("Leipzig", "04109")
        )
        val dataCopy = data.toMap()

        val source = MapOfAddresses(addresses = data)

        val result = source.mutate {
            addresses["B"] = Address("Bonn", "53111")
        }


        withClue("Source object must NOT be modified") {
            source shouldNotBeSameInstanceAs result
            source.addresses shouldBe dataCopy
            source.addresses shouldBeSameInstanceAs data
        }

        withClue("First list element must be modified") {
            source.addresses["B"] shouldNotBe result.addresses["B"]
        }

        withClue("Second list element must stay the same") {
            source.addresses["L"] shouldBe result.addresses["L"]
            source.addresses["L"] shouldBeSameInstanceAs result.addresses["L"]
        }

        withClue("Result must be modified properly") {

            result.addresses shouldBe mapOf(
                "B" to Address("Bonn", "53111"),
                "L" to Address("Leipzig", "04109")
            )
        }
    }

    "Mutating a map by adding an element via set" {

        val data = mapOf(
            "B" to Address("Berlin", "10115"),
            "L" to Address("Leipzig", "04109")
        )
        val dataCopy = data.toMap()

        val source = MapOfAddresses(addresses = data)

        val result = source.mutate {
            addresses["C"] = Address("Chemnitz", "09111")
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.addresses shouldBe dataCopy
                source.addresses shouldBeSameInstanceAs data
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

        val data = mapOf(
            "B" to Address("Berlin", "10115"),
            "L" to Address("Leipzig", "04109")
        )
        val dataCopy = data.toMap()

        val source = MapOfAddresses(addresses = data)

        val result = source.mutate {
            addresses.put(
                "C" to Address("Chemnitz", "09111"),
                "O" to Address("Bonn", "53111")
            )
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.addresses shouldBe dataCopy
                source.addresses shouldBeSameInstanceAs data
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

        val data = mapOf(
            "B" to Address("Berlin", "10115"),
            "L" to Address("Leipzig", "04109")
        )
        val dataCopy = data.toMap()

        val source = MapOfAddresses(addresses = data)

        val result = source.mutate {
            addresses.forEach { (_, address) ->
                address.city += " x"
            }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.addresses shouldBe dataCopy
                source.addresses shouldBeSameInstanceAs data
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

        val data = mapOf(
            "B" to Address("Berlin", "10115"),
            "L" to Address("Leipzig", "04109")
        )
        val dataCopy = data.toMap()

        val source = MapOfAddresses(addresses = data)

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
                source shouldNotBeSameInstanceAs result
                source.addresses shouldBe dataCopy
                source.addresses shouldBeSameInstanceAs data
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

        val data = mapOf(
            "B" to Address("Berlin", "10115"),
            "L" to Address("Leipzig", "04109")
        )
        val dataCopy = data.toMap()

        val source = MapOfAddresses(addresses = data)

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
                result.addresses shouldBe mapOf()
            }
        }
    }

    "Mutating a map by removing elements via filter" {

        val data = mapOf(
            "B" to Address("Berlin", "10115"),
            "L" to Address("Leipzig", "04109")
        )
        val dataCopy = data.toMap()

        val source = MapOfAddresses(addresses = data)

        val result = source.mutate {
            addresses += addresses.filter { it.value.city == "Leipzig" }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.addresses shouldBe dataCopy
                source.addresses shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe mapOf(
                    "L" to Address("Leipzig", "04109")
                )
            }
        }
    }

    "Mutating some elements in a map via filter and forEach" {

        val data = mapOf(
            "B" to Address("Berlin", "10115"),
            "L" to Address("Leipzig", "04109"),
            "C" to Address("Chemnitz", "09111"),
            "O" to Address("Bonn", "53111")
        )
        val dataCopy = data.toMap()

        val source = MapOfAddresses(addresses = data)

        val result = source.mutate {
            addresses
                .filter { (k, v) -> k == "L" || v.city.startsWith("B") }
                .forEach { it.value.city += " x" }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.addresses shouldBe dataCopy
                source.addresses shouldBeSameInstanceAs data
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

        val data = mapOf(
            "B" to Address("Berlin", "10115"),
            "L" to Address("Leipzig", "04109")
        )
        val dataCopy = data.toMap()

        val source = MapOfAddresses(addresses = data)

        val result = source.mutate {
            addresses += mapOf(
                "C" to Address("Chemnitz", "09111")
            )
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.addresses shouldBe dataCopy
                source.addresses shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe mapOf(
                    "C" to Address("Chemnitz", "09111")
                )
            }
        }
    }

    "Mutating a map by removing elements via removeWhere" {

        val data = mapOf(
            "B" to Address("Berlin", "10115"),
            "L" to Address("Leipzig", "04109"),
            "C" to Address("Chemnitz", "09111")
        )
        val dataCopy = data.toMap()

        val source = MapOfAddresses(addresses = data)

        val result = source.mutate {
            addresses.removeWhere { it.value.city == "Leipzig" || it.key == "C" }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.addresses shouldBe dataCopy
                source.addresses shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe mapOf(
                    "B" to Address("Berlin", "10115")
                )
            }
        }
    }

    "Mutating a map by removing elements via retainWhere" {

        val data = mapOf(
            "B" to Address("Berlin", "10115"),
            "L" to Address("Leipzig", "04109"),
            "C" to Address("Chemnitz", "09111")
        )
        val dataCopy = data.toMap()

        val source = MapOfAddresses(addresses = data)

        val result = source.mutate {
            addresses.retainWhere { (k, v) -> v.city == "Leipzig" || k == "C" }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.addresses shouldBe dataCopy
                source.addresses shouldBeSameInstanceAs data
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

        val data = mapOf(
            "B" to Address("Berlin", "10115"),
            "L" to Address("Leipzig", "04109"),
            "C" to Address("Chemnitz", "09111")
        )
        val dataCopy = data.toMap()

        val source = MapOfAddresses(addresses = data)

        val result = source.mutate {
            addresses.remove("B", "C")
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.addresses shouldBe dataCopy
                source.addresses shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.addresses shouldBe mapOf(
                    "L" to Address("Leipzig", "04109")
                )
            }
        }
    }
})
