package de.peekandpoke.ultra.mutator.e2e

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.DisplayName
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import java.time.LocalDate
import kotlin.reflect.KMutableProperty0

@DisplayName("E2E - AnyMutationsSpec")
class AnyMutationsSpec : StringSpec({

    fun <T> setProp(prop: KMutableProperty0<T>, v: T) = prop.set(v)

    "Mutating but not changing an 'Any' value is treated like no mutation" {

        val source = WithAnyObject(
            anObject = "string"
        )

        val result = source.mutate {
            anObject = "string"
        }

        assertSoftly {

            withClue("When all mutations are not changing an 'Any' value, the source object must be returned") {
                source shouldBeSameInstanceAs result
            }
        }
    }

    "Mutating one 'kotlin.Any' property by assignment" {

        val source = WithAnyObject(anObject = 0)

        val result = source.mutate {
            anObject = "changed"
        }

        assertSoftly {

            source shouldNotBeSameInstanceAs result

            withClue("Source object must NOT be modified") {
                source.anObject shouldBe 0
            }

            withClue("Result must be modified properly") {
                result.anObject shouldBe "changed"
            }
        }
    }

    "Mutating one 'kotlin.Any' property via reflection" {

        val source = WithAnyObject(anObject = 0)

        val result = source.mutate {
            setProp(::anObject, "changed")
        }

        assertSoftly {

            source shouldNotBeSameInstanceAs result

            withClue("Source object must NOT be modified") {
                source.anObject shouldBe 0
            }

            withClue("Result must be modified properly") {
                result.anObject shouldBe "changed"
            }
        }
    }

    "Mutating one 'LocalDate' property by assignment" {

        val source = WithDate(date = LocalDate.MIN)

        val result = source.mutate {
            date = LocalDate.MAX
        }

        assertSoftly {

            source shouldNotBeSameInstanceAs result

            withClue("Source object must NOT be modified") {
                source.date shouldBe LocalDate.MIN
            }

            withClue("Result must be modified properly") {
                result.date shouldBe LocalDate.MAX
            }
        }
    }

    "Mutating one 'LocalDate' property via reflection" {

        val source = WithDate(date = LocalDate.MIN)

        val result = source.mutate {
            setProp(::date, LocalDate.MAX)
        }

        assertSoftly {

            source shouldNotBeSameInstanceAs result

            withClue("Source object must NOT be modified") {
                source.date shouldBe LocalDate.MIN
            }

            withClue("Result must be modified properly") {
                result.date shouldBe LocalDate.MAX
            }
        }
    }
})
