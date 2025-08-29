package de.peekandpoke.mutator.e2e

import de.peekandpoke.mutator.domain.Address
import de.peekandpoke.mutator.domain.OneBoundedGenericParam
import de.peekandpoke.mutator.domain.OneGenericParam
import de.peekandpoke.mutator.domain.Outer
import de.peekandpoke.mutator.domain.Person
import de.peekandpoke.mutator.domain.SealedInterface
import de.peekandpoke.mutator.domain.TwoGenericParams
import de.peekandpoke.mutator.domain.address
import de.peekandpoke.mutator.domain.city
import de.peekandpoke.mutator.domain.firstName
import de.peekandpoke.mutator.domain.inner
import de.peekandpoke.mutator.domain.lastName
import de.peekandpoke.mutator.domain.mutate
import de.peekandpoke.mutator.domain.street
import de.peekandpoke.mutator.domain.value
import de.peekandpoke.mutator.domain.value1
import de.peekandpoke.mutator.domain.value2
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

        "Mutating Sealed interfaces must work" {
            val subject: SealedInterface = SealedInterface.One(value = "1")

            val result = subject.mutate {
                when (val v = get()) {
                    is SealedInterface.One -> cast(v) {
                        value = "1+1"
                    }

                    is SealedInterface.Two -> cast(v) {
                        value += value
                    }
                }
            }

            result shouldBe SealedInterface.One(value = "1+1")
        }

        "Mutating a generic class with one unbounded generic parameter must work" {
            // With string
            val subjectStr = OneGenericParam(value = "1")
            val resultStr = subjectStr.mutate {
                value = "1+1"
            }
            resultStr shouldBe OneGenericParam(value = "1+1")

            // With int

            val subjectInt = OneGenericParam(value = 1)
            val resultInt = subjectInt.mutate {
                value += 10
            }
            resultInt shouldBe OneGenericParam(value = 1 + 10)
        }

        "Mutating a generic class with two unbounded generic parameters must work" {
            val subject = TwoGenericParams(value1 = "1", value2 = 1)
            val result = subject.mutate {
                value1 = "1+1"
                value2 += 10
            }
            result shouldBe TwoGenericParams(value1 = "1+1", value2 = 1 + 10)
        }

        "Mutating a generic class with one bounded generic parameter must work" {
            val subject = OneBoundedGenericParam(value = "1")
            val result = subject.mutate {
                value = "1+1"
            }
            result shouldBe OneBoundedGenericParam(value = "1+1")
        }

        "Mutating a list of sealed interfaces must work" {

            val subject = listOf(
                SealedInterface.One(value = "1"),
                SealedInterface.Two(value = 1),
            )

            val result = subject.mutate {
                forEach {
                    with(it) {
                        when (val v = get()) {
                            is SealedInterface.One -> cast(v) {
                                value = "1+1"
                            }

                            is SealedInterface.Two -> cast(v) {
                                value += 10
                            }
                        }
                    }
                }
            }

            result shouldBe listOf(
                SealedInterface.One(value = "1+1"),
                SealedInterface.Two(value = 1 + 10),
            )
        }

        "Mutating a set of sealed interfaces must work" {

            val subject = setOf(
                SealedInterface.One(value = "1"),
                SealedInterface.Two(value = 1),
            )

            val result = subject.mutate {
                forEach {
                    with(it) {
                        when (val v = get()) {
                            is SealedInterface.One -> cast(v) {
                                value = "1+1"
                            }

                            is SealedInterface.Two -> cast(v) {
                                value += 10
                            }
                        }
                    }
                }
            }

            result shouldBe setOf(
                SealedInterface.One(value = "1+1"),
                SealedInterface.Two(value = 1 + 10),
            )
        }
    }
}
