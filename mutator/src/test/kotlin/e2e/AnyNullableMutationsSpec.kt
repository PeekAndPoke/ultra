package de.peekandpoke.ultra.mutator.e2e

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.DisplayName
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import kotlin.reflect.KMutableProperty0

@DisplayName("E2E - AnyNullableMutationsSpec")
class AnyNullableMutationsSpec : StringSpec({

    fun <T> setProp(prop: KMutableProperty0<T>, v: T) = prop.set(v)

    "Mutating but not changing any value is treated like no mutation" {

        val source = WithAnyNullableObject(
            anObject = null
        )

        val result = source.mutate {
            anObject = null
        }

        assertSoftly {

            withClue("When all mutations are not changing any value, the source object must be returned") {
                source shouldBeSameInstanceAs result
            }
        }
    }

    "Mutating one 'kotlin.Any?' property by assignment" {

        val source = WithAnyNullableObject(anObject = "something")

        val result = source.mutate {
            anObject = null
        }

        assertSoftly {

            source shouldNotBeSameInstanceAs result

            withClue("Source object must NOT be modified") {
                source.anObject shouldBe "something"
            }

            withClue("Result must be modified properly") {
                result.anObject shouldBe null
            }
        }
    }

    "Mutating one 'kotlin.Any?' property via reflection" {

        val source = WithAnyNullableObject(anObject = 0)

        val result = source.mutate {
            setProp(::anObject, null)
        }

        assertSoftly {

            source shouldNotBeSameInstanceAs result

            withClue("Source object must NOT be modified") {
                source.anObject shouldBe 0
            }

            withClue("Result must be modified properly") {
                result.anObject shouldBe null
            }
        }
    }
})
