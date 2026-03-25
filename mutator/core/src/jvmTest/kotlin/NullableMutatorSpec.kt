package io.peekandpoke.mutator

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.peekandpoke.mutator.domain.Address
import io.peekandpoke.mutator.domain.PersonWithNullableAddress
import io.peekandpoke.mutator.domain.address
import io.peekandpoke.mutator.domain.city
import io.peekandpoke.mutator.domain.mutate
import io.peekandpoke.mutator.domain.mutator
import io.peekandpoke.mutator.domain.name
import io.peekandpoke.mutator.domain.street

class NullableMutatorSpec : StringSpec({

    "mutating a null nullable property returns null mutator" {
        val person = PersonWithNullableAddress(name = "John", address = null)
        val mutator = person.mutator()

        mutator.address.shouldBeNull()
    }

    "mutating a non-null nullable property works" {
        val person = PersonWithNullableAddress(
            name = "John",
            address = Address("Street", "City", "Zip"),
        )

        val result = person.mutate {
            address?.street = "New Street"
        }

        result.address shouldNotBe null
        result.address?.street shouldBe "New Street"
        result.address?.city shouldBe "City"
    }

    "setting name on person with null address preserves null" {
        val person = PersonWithNullableAddress(name = "John", address = null)

        val result = person.mutate {
            name = "Jane"
        }

        result.name shouldBe "Jane"
        result.address.shouldBeNull()
    }

    "mutating non-null address to a different value" {
        val person = PersonWithNullableAddress(
            name = "John",
            address = Address("Street 1", "City 1", "Zip 1"),
        )

        val result = person.mutate {
            address?.city = "City 2"
            address?.street = "Street 2"
        }

        result.address shouldBe Address("Street 2", "City 2", "Zip 1")
    }

    "no mutation on null address returns same instance" {
        val person = PersonWithNullableAddress(name = "John", address = null)

        val result = person.mutate {
            // accessing nullable address that is null - no-op
            address
        }

        result shouldBe person
    }

    "no mutation on non-null address returns same instance" {
        val person = PersonWithNullableAddress(
            name = "John",
            address = Address("Street", "City", "Zip"),
        )

        val result = person.mutate {
            // no actual changes
        }

        result shouldBe person
    }
})
