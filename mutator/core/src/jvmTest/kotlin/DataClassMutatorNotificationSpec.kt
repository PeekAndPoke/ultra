package io.peekandpoke.mutator

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.mutator.domain.Address
import io.peekandpoke.mutator.domain.Person
import io.peekandpoke.mutator.domain.address
import io.peekandpoke.mutator.domain.age
import io.peekandpoke.mutator.domain.city
import io.peekandpoke.mutator.domain.mutator
import io.peekandpoke.mutator.domain.street

class DataClassMutatorNotificationsSpec : StringSpec({

    "Modifying a primitive property should notify" {
        val subject = Address(street = "A", city = "B", zip = "C")
        val mutator = subject.mutator()

        var notified = 0
        mutator.onChange { notified++ }

        mutator.street = "A-changed"

        notified shouldBe 1
    }

    "Setting a primitive property to the same value should NOT notify" {
        val subject = Address(street = "A", city = "B", zip = "C")
        val mutator = subject.mutator()

        var notified = 0
        mutator.onChange { notified++ }

        mutator.street = "A"

        notified shouldBe 0
    }

    "Multiple modifications should notify multiple times" {
        val subject = Address(street = "A", city = "B", zip = "C")
        val mutator = subject.mutator()

        var notified = 0
        mutator.onChange { notified++ }

        mutator.street = "A-changed"
        mutator.city = "B-changed"

        notified shouldBe 2
    }

    "Modifying a property and then reverting it should notify twice" {
        val subject = Address(street = "A", city = "B", zip = "C")
        val mutator = subject.mutator()

        var notified = 0
        mutator.onChange { notified++ }

        mutator.street = "A-changed"
        mutator.street = "A"

        notified shouldBe 2
        // Even though it notified twice, the overall object is not modified compared to the original
        mutator.isModified() shouldBe false
    }

    "Modifying a nested data class property directly should notify the parent" {
        val subject = Person(
            firstName = "John",
            lastName = "Doe",
            age = 42,
            address = Address(street = "street", city = "city", zip = "zip")
        )

        val mutator = subject.mutator()

        var notified = 0
        mutator.onChange { notified++ }

        // We modify the nested address's street
        mutator.address.street = "newStreet"

        // The parent mutator should have been notified
        notified shouldBe 1
    }

    "Setting a nested data class property to the same value should NOT notify the parent" {
        val subject = Person(
            firstName = "John",
            lastName = "Doe",
            age = 42,
            address = Address(street = "street", city = "city", zip = "zip")
        )

        val mutator = subject.mutator()

        var notified = 0
        mutator.onChange { notified++ }

        // We modify the nested address's street to exactly what it already was
        mutator.address.street = "street"

        // The parent mutator should NOT be notified
        notified shouldBe 0
    }

    "Modifying a nested data class using context block should notify the parent" {
        val subject = Person(
            firstName = "John",
            lastName = "Doe",
            age = 42,
            address = Address(street = "street", city = "city", zip = "zip")
        )

        val mutator = subject.mutator()

        var notified = 0
        mutator.onChange { notified++ }

        mutator {
            address {
                street = "newStreet"
                city = "newCity"
            }
            age = 43
        }

        // It notified 1 time for street, 1 time for city (which bubbles up),
        // and 1 time for age. Total = 3 notifications.
        notified shouldBe 3
    }
})
