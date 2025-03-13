package de.peekandpoke.ultra.mutator.e2e

import io.kotest.assertions.withClue
import io.kotest.core.spec.DisplayName
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

@DisplayName("E2E - NestedObjectMutationsSpec")
class NestedObjectMutationsSpec : StringSpec({

    "Mutating properties of root object only" {

        val source = Company(
            "Corp",
            Person(
                "Sam", 25,
                Address("Berlin", "10115")
            )
        )

        val result = source.mutate {

            name = name.plus("oration").uppercase()
        }

        withClue("Source object must NOT be modified") {
            source shouldNotBe result
        }

        withClue("Nested object must stay identical, as it was not modified") {
            source.boss shouldBe result.boss
        }

        withClue("Result must be modified properly") {
            result.name shouldBe "CORPORATION"
        }
    }

    "Mutating properties of a nested object" {

        val source = Company(
            "Corp",
            Person(
                "Sam", 25,
                Address("Berlin", "10115")
            )
        )

        val result = source.mutate {

            name = name.plus("oration").uppercase()

            boss.address.apply {
                city = "Leipzig"
                zip = "04109"
            }
        }

        withClue("Source object must NOT be modified") {
            source shouldNotBe result
        }

        withClue("Result must be modified properly") {
            result.name shouldBe "CORPORATION"

            result.boss shouldNotBe source.boss

            result.boss.address shouldNotBe source.boss.address
            result.boss.address shouldBe Address("Leipzig", "04109")
        }
    }

    "Mutating an object by replacing a nested object" {

        val source = Company(
            "Corp",
            Person(
                "Sam", 25,
                Address("Berlin", "10115")
            )
        )

        val result = source.mutate {
            boss.address += Address("Leipzig", "04109")
        }

        withClue("Source object must NOT be modified") {
            source shouldNotBe result
        }

        withClue("Result must be modified properly") {
            result.boss shouldNotBe source.boss

            result.boss.address shouldNotBe source.boss.address
            result.boss.address shouldBe Address("Leipzig", "04109")
        }
    }

    "Mutating an object by replacing a property mutator directly" {

        val source = Company(
            "Corp",
            Person(
                "Sam", 25,
                Address("Berlin", "10115")
            )
        )

        val result = source.mutate {
            // First we fully replace the "address"
            boss.address = Address("Leipzig", "04109").mutator()
        }

        withClue("Source object must NOT be modified") {
            source shouldNotBe result
        }

        withClue("Result must be modified properly") {
            result.boss shouldNotBe source.boss

            result.boss.address shouldNotBe source.boss.address
            result.boss.address shouldBe Address("Leipzig", "04109")
        }
    }

    "Mutating an object by replacing a property mutator directly (and modifying it again)" {

        val source = Company(
            "Corp",
            Person(
                "Sam", 25,
                Address("Berlin", "10115")
            )
        )

        val result = source.mutate {
            // First we fully replace the "address"
            boss.address = Address("Leipzig", "04109").mutator()
            // Then we modify the property again
            boss.address.zip = "04110"
        }

        withClue("Source object must NOT be modified") {
            source shouldNotBe result
        }

        withClue("Result must be modified properly") {
            result.boss shouldNotBe source.boss

            result.boss.address shouldNotBe source.boss.address
            result.boss.address shouldBe Address("Leipzig", "04110")
        }
    }
})
