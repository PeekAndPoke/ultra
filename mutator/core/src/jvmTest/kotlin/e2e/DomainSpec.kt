package de.peekandpoke.mutator.e2e

import de.peekandpoke.mutator.domain.Address
import de.peekandpoke.mutator.domain.Outer
import de.peekandpoke.mutator.domain.Person
import de.peekandpoke.mutator.domain.address
import de.peekandpoke.mutator.domain.city
import de.peekandpoke.mutator.domain.firstName
import de.peekandpoke.mutator.domain.inner
import de.peekandpoke.mutator.domain.lastName
import de.peekandpoke.mutator.domain.mutate
import de.peekandpoke.mutator.domain.street
import de.peekandpoke.mutator.domain.value
import de.peekandpoke.mutator.domain.zip
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class DomainSpec : StringSpec() {

    init {
        "Test mutation of Address class" {

            val subject = Address(street = "street", city = "city", zip = "zip")

            val result = subject.mutate {
                street = "newStreet"
                city = "newCity"
                zip = "newZip"
            }

            result shouldBe Address(street = "newStreet", city = "newCity", zip = "newZip")
        }

        "Test mutation of Person and Address" {

            val subject = Person(
                firstName = "John",
                lastName = "Doe",
                age = 42,
                address = Address(street = "street", city = "city", zip = "zip"),
            )

            val result = subject.mutate {
                firstName += "+1"
                lastName += "+2"
                address {
                    street += "+3"
                }
            }

            result shouldBe Person(
                firstName = "John+1",
                lastName = "Doe+2",
                age = 42,
                address = Address(street = "street+3", city = "city", zip = "zip"),
            )
        }

        "Test creation of Outer with Inner" {
            val inner = Outer.Inner(value = 42)
            val outer = Outer(inner = inner)

            outer.inner.value shouldBe 42
        }

        "Test mutation of Inner value" {
            val subject = Outer(
                inner = Outer.Inner(value = 10)
            )

            val result = subject.mutate {
                inner {
                    value = 20
                }
            }

            result shouldBe Outer(
                inner = Outer.Inner(value = 20)
            )
        }

        "Test mutation of Inner by replacing the entire Inner object" {
            val subject = Outer(
                inner = Outer.Inner(value = 10)
            )

            val result = subject.mutate {
                inner.modifyValue { Outer.Inner(value = 30) }
            }

            result shouldBe Outer(
                inner = Outer.Inner(value = 30)
            )
        }

        "Test that original object remains unchanged after mutation" {
            val originalInner = Outer.Inner(value = 100)
            val subject = Outer(inner = originalInner)

            val result = subject.mutate {
                inner {
                    value = 200
                }
            }

            // Original should remain unchanged
            subject.inner.value shouldBe 100
            // Result should have the new value
            result.inner.value shouldBe 200
        }

        "Test multiple mutations on the same object" {
            val subject = Outer(
                inner = Outer.Inner(value = 5)
            )

            val firstMutation = subject.mutate {
                inner {
                    value = 10
                }
            }

            val secondMutation = firstMutation.mutate {
                inner {
                    value = 15
                }
            }

            subject.inner.value shouldBe 5
            firstMutation.inner.value shouldBe 10
            secondMutation.inner.value shouldBe 15
        }
    }
}
