package io.peekandpoke.mutator.e2e

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.mutator.add
import io.peekandpoke.mutator.domain.Address
import io.peekandpoke.mutator.domain.WithCollections
import io.peekandpoke.mutator.domain.city
import io.peekandpoke.mutator.domain.lookup
import io.peekandpoke.mutator.domain.mutate
import io.peekandpoke.mutator.domain.street
import io.peekandpoke.mutator.domain.tags

class CollectionPropertiesSpec : StringSpec({

    val addr1 = Address("Street 1", "City 1", "Zip 1")
    val addr2 = Address("Street 2", "City 2", "Zip 2")
    val addr3 = Address("Street 3", "City 3", "Zip 3")

    // Set property tests //////////////////////////////////////////////////////////////////////////////////

    "mutating a Set property - modifying an element via iteration" {
        val obj = WithCollections(
            tags = setOf(addr1, addr2),
            lookup = emptyMap(),
        )

        val result = obj.mutate {
            tags.forEach { mutator ->
                if (mutator.get().street == "Street 1") {
                    mutator.street = "Modified Street"
                }
            }
        }

        result.tags shouldBe setOf(
            Address("Modified Street", "City 1", "Zip 1"),
            addr2,
        )
    }

    "mutating a Set property - adding an element" {
        val obj = WithCollections(
            tags = setOf(addr1),
            lookup = emptyMap(),
        )

        val result = obj.mutate {
            tags.add(addr2)
        }

        result.tags shouldBe setOf(addr1, addr2)
    }

    "mutating a Set property - clearing" {
        val obj = WithCollections(
            tags = setOf(addr1, addr2),
            lookup = emptyMap(),
        )

        val result = obj.mutate {
            tags.clear()
        }

        result.tags shouldBe emptySet()
    }

    "mutating a Set property - removing via iterator" {
        val obj = WithCollections(
            tags = setOf(addr1, addr2),
            lookup = emptyMap(),
        )

        val result = obj.mutate {
            val iter = tags.iterator()
            while (iter.hasNext()) {
                val m = iter.next()
                if (m.get().street == "Street 1") {
                    iter.remove()
                }
            }
        }

        result.tags shouldBe setOf(addr2)
    }

    // Map property tests //////////////////////////////////////////////////////////////////////////////////

    "mutating a Map property - modifying a value" {
        val obj = WithCollections(
            tags = emptySet(),
            lookup = mapOf("a" to addr1, "b" to addr2),
        )

        val result = obj.mutate {
            lookup["a"]?.street = "Modified Street"
        }

        result.lookup shouldBe mapOf(
            "a" to Address("Modified Street", "City 1", "Zip 1"),
            "b" to addr2,
        )
    }

    "mutating a Map property - modifying via entries" {
        val obj = WithCollections(
            tags = emptySet(),
            lookup = mapOf("a" to addr1, "b" to addr2),
        )

        val result = obj.mutate {
            lookup.entries.forEach { entry ->
                if (entry.key == "b") {
                    entry.value.city = "New City"
                }
            }
        }

        result.lookup shouldBe mapOf(
            "a" to addr1,
            "b" to Address("Street 2", "New City", "Zip 2"),
        )
    }

    "mutating a Map property - removing an entry" {
        val obj = WithCollections(
            tags = emptySet(),
            lookup = mapOf("a" to addr1, "b" to addr2),
        )

        val result = obj.mutate {
            lookup.remove("a")
        }

        result.lookup shouldBe mapOf("b" to addr2)
    }

    "mutating a Map property - clearing" {
        val obj = WithCollections(
            tags = emptySet(),
            lookup = mapOf("a" to addr1, "b" to addr2),
        )

        val result = obj.mutate {
            lookup.clear()
        }

        result.lookup shouldBe emptyMap()
    }

    "no mutation returns same instance" {
        val obj = WithCollections(
            tags = setOf(addr1),
            lookup = mapOf("a" to addr2),
        )

        val result = obj.mutate { }

        result shouldBe obj
    }

    // Combined mutations //////////////////////////////////////////////////////////////////////////////////

    "mutating both Set and Map properties in one mutation" {
        val obj = WithCollections(
            tags = setOf(addr1),
            lookup = mapOf("a" to addr2),
        )

        val result = obj.mutate {
            tags.add(addr3)
            lookup["a"]?.street = "Changed"
        }

        result.tags shouldBe setOf(addr1, addr3)
        result.lookup shouldBe mapOf(
            "a" to Address("Changed", "City 2", "Zip 2"),
        )
    }
})
