package de.peekandpoke.ultra.mutator.e2e

import io.kotest.assertions.withClue
import io.kotest.core.spec.DisplayName
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.reflect.KMutableProperty0

@DisplayName("E2E - ScalarMutationsSpec")
class ScalarMutationsSpec : StringSpec({

    fun <T> setProp(prop: KMutableProperty0<T>, v: T) = prop.set(v)

    "Mutating nothing will not clone the source" {

        val source = WithScalars()

        val result = source.mutate { }

        withClue("When nothing was changed the source object must be returned") {
            (source === result) shouldBe true
        }
    }

    "Mutating but not changing any value is treated like no mutation" {

        val source = WithScalars(
            aString = "string",
            aChar = 'c',
            aByte = 1,
            aShort = 1,
            aInt = 1,
            aLong = 1L,
            aFloat = 1.0f,
            aDouble = 1.0,
            aBool = true
        )

        val result = source.mutate {
            aString = "string"
            aChar = 'c'
            aByte = 1
            aShort = 1
            aInt = 1
            aLong = 1L
            aFloat = 1.0f
            aDouble = 1.0
            aBool = true
        }

        withClue("When all mutations are not changing any value, the source object must be returned") {
            (source === result) shouldBe true
        }
    }

    "Mutating one scalar property by assignment" {

        val source = WithScalars()

        val result = source.mutate {
            aString = "changed"
        }

        source shouldNotBe result

        withClue("Source object must NOT be modified") {
            source.aString shouldBe "string"
            source.aChar shouldBe 'c'
            source.aByte shouldBe 1.toByte()
            source.aShort shouldBe 1.toShort()
            source.aInt shouldBe 1
            source.aLong shouldBe 1L
            source.aFloat shouldBe 1.0f
            source.aDouble shouldBe 1.0
            source.aBool shouldBe true
        }

        withClue("Result must be modified properly") {
            result.aString shouldBe "changed"
            result.aChar shouldBe 'c'
            result.aByte shouldBe 1.toByte()
            result.aShort shouldBe 1.toShort()
            result.aInt shouldBe 1
            result.aLong shouldBe 1L
            result.aFloat shouldBe 1.0f
            result.aDouble shouldBe 1.0
            result.aBool shouldBe true
        }
    }

    "Mutating one scalar property via callback" {

        val source = WithScalars()

        val result = source.mutate {
            aString = aString.toUpperCase()
        }

        source shouldNotBe result

        withClue("Source object must NOT be modified") {
            source.aString shouldBe "string"
        }

        withClue("Result must be modified properly") {
            result.aString shouldBe "STRING"
        }
    }

    "Mutating one scalar property via reflection" {

        val source = WithScalars()

        val result = source.mutate {
            @Suppress("RemoveExplicitTypeArguments")
            setProp<String>(::aString, "changed")
        }

        source shouldNotBe result

        withClue("Source object must NOT be modified") {
            source.aString shouldBe "string"
        }

        withClue("Result must be modified properly") {
            result.aString shouldBe "changed"
        }
    }

    "Mutating multiple scalars in multiple ways" {

        val source = WithScalars()

        val result = source.mutate {
            aString = aString.plus(" plus")
            aChar = 'd'
            aByte = 2
            aShort = 3
            aInt *= 4
            aLong -= 2
            aFloat /= 2
            aDouble *= 3.5
            @Suppress("RemoveExplicitTypeArguments")
            setProp<Boolean>(::aBool, false)
        }

        source shouldNotBe result

        withClue("Source object must NOT be modified") {
            source.aString shouldBe "string"
            source.aChar shouldBe 'c'
            source.aByte shouldBe 1.toByte()
            source.aShort shouldBe 1.toShort()
            source.aInt shouldBe 1
            source.aLong shouldBe 1L
            source.aFloat shouldBe 1.0f
            source.aDouble shouldBe 1.0
            source.aBool shouldBe true
        }

        withClue("Result must be modified properly") {
            result.aString shouldBe "string plus"
            result.aChar shouldBe 'd'
            result.aByte shouldBe 2.toByte()
            result.aShort shouldBe 3.toShort()
            result.aInt shouldBe 4
            result.aLong shouldBe -1L
            result.aFloat shouldBe 0.5f
            result.aDouble shouldBe 3.5
            result.aBool shouldBe false
        }
    }
})
