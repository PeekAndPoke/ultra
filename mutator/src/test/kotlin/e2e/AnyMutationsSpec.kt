package de.peekandpoke.ultra.mutator.e2e

import io.kotlintest.DisplayName
import io.kotlintest.assertSoftly
import io.kotlintest.matchers.withClue
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.StringSpec
import kotlin.reflect.KMutableProperty0

@DisplayName("E2E - AnyMutationsSpec")
class AnyMutationsSpec : StringSpec({

    fun <T> setProp(prop: KMutableProperty0<T>, v: T) = prop.set(v)

    "Mutating but not changing any value is treated like no mutation" {

        val source = WithAnyObject(
            anObject = "string"
        )

        val result = source.mutate {
            anObject = "string"
        }

        assertSoftly {

            withClue("When all mutations are not changing any value, the source object must be returned") {
                (source === result) shouldBe true
            }
        }
    }

    "Mutating one 'kotlin.Any' property by assignment" {

        val source = WithAnyObject(anObject = 0)

        val result = source.mutate {
            anObject = "changed"
        }

        assertSoftly {

            source shouldNotBe result

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
            @Suppress("RemoveExplicitTypeArguments")
            setProp<Any>(::anObject, "changed")
        }

        assertSoftly {

            source shouldNotBe result

            withClue("Source object must NOT be modified") {
                source.anObject shouldBe 0
            }

            withClue("Result must be modified properly") {
                result.anObject shouldBe "changed"
            }
        }
    }
})
