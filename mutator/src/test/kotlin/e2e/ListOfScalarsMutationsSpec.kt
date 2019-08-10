package de.peekandpoke.ultra.mutator.e2e

import io.kotlintest.DisplayName
import io.kotlintest.assertSoftly
import io.kotlintest.matchers.withClue
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.StringSpec

@DisplayName("E2E - ListOfScalarsMutationsSpec")
class ListOfScalarsMutationsSpec : StringSpec({

    ////  List of bools  //////////////////////////////////////////////////////////////////////

    "Mutating a list of bools by removing elements via removeWhere" {

        val source = ListOfBools(values = listOf(true, false, true))

        val result = source.mutate {
            values.removeWhere { this }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.values shouldNotBe result.values
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf(false)
            }
        }
    }

    "Mutating a list of bools by removing elements via retainWhere" {

        val source = ListOfBools(values = listOf(true, false, true))

        val result = source.mutate {
            values.retainWhere { this }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.values shouldNotBe result.values
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf(true, true)
            }
        }
    }

    ////  List of chars  //////////////////////////////////////////////////////////////////////

    "Mutating a list of chars by removing elements via removeWhere" {

        val source = ListOfChars(values = listOf('A', 'B', 'C'))

        val result = source.mutate {
            values.removeWhere { this > 'A' }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.values shouldNotBe result.values
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf('A')
            }
        }
    }

    "Mutating a list of chars by removing elements via retainWhere" {

        val source = ListOfChars(values = listOf('A', 'B', 'C'))

        val result = source.mutate {
            values.retainWhere { this > 'A' }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.values shouldNotBe result.values
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf('B', 'C')
            }
        }
    }

    ////  List of bytes  //////////////////////////////////////////////////////////////////////

    "Mutating a list of bytes by removing elements via removeWhere" {

        val source = ListOfBytes(values = listOf(1, 2, 3))

        val result = source.mutate {
            values.removeWhere { this > 1 }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.values shouldNotBe result.values
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf<Byte>(1)
            }
        }
    }

    "Mutating a list of bytes by removing elements via retainWhere" {

        val source = ListOfBytes(values = listOf(1, 2, 3))

        val result = source.mutate {
            values.retainWhere { this > 1 }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.values shouldNotBe result.values
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf<Byte>(2, 3)
            }
        }
    }

    ////  List of shorts  //////////////////////////////////////////////////////////////////////

    "Mutating a list of shorts by removing elements via removeWhere" {

        val source = ListOfShorts(values = listOf(1, 2, 3))

        val result = source.mutate {
            values.removeWhere { this > 1 }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.values shouldNotBe result.values
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf<Short>(1)
            }
        }
    }

    "Mutating a list of shorts by removing elements via retainWhere" {

        val source = ListOfShorts(values = listOf(1, 2, 3))

        val result = source.mutate {
            values.retainWhere { this > 1 }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.values shouldNotBe result.values
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf<Short>(2, 3)
            }
        }
    }

    ////  List of ints  //////////////////////////////////////////////////////////////////////

    "Mutating a list of ints by removing elements via removeWhere" {

        val source = ListOfInts(values = listOf(1, 2, 3))

        val result = source.mutate {
            values.removeWhere { this > 1 }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.values shouldNotBe result.values
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf(1)
            }
        }
    }

    "Mutating a list of ints by removing elements via retainWhere" {

        val source = ListOfInts(values = listOf(1, 2, 3))

        val result = source.mutate {
            values.retainWhere { this > 1 }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.values shouldNotBe result.values
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf(2, 3)
            }
        }
    }

    ////  List of longs  //////////////////////////////////////////////////////////////////////

    "Mutating a list of longs by removing elements via removeWhere" {

        val source = ListOfLongs(values = listOf(1, 2, 3))

        val result = source.mutate {
            values.removeWhere { this > 1 }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.values shouldNotBe result.values
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf<Long>(1)
            }
        }
    }

    "Mutating a list of longs by removing elements via retainWhere" {

        val source = ListOfLongs(values = listOf(1, 2, 3))

        val result = source.mutate {
            values.retainWhere { this > 1 }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.values shouldNotBe result.values
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf<Long>(2, 3)
            }
        }
    }

    ////  List of floats  //////////////////////////////////////////////////////////////////////

    "Mutating a list of floats by removing elements via removeWhere" {

        val source = ListOfFloats(values = listOf(1.1f, 2.2f, 3.3f))

        val result = source.mutate {
            values.removeWhere { this > 1.1f }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.values shouldNotBe result.values
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf(1.1f)
            }
        }
    }

    "Mutating a list of floats by removing elements via retainWhere" {

        val source = ListOfFloats(values = listOf(1.1f, 2.2f, 3.3f))

        val result = source.mutate {
            values.retainWhere { this > 1.1f }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.values shouldNotBe result.values
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf(2.2f, 3.3f)
            }
        }
    }

    ////  List of doubles  //////////////////////////////////////////////////////////////////////

    "Mutating a list of doubles by removing elements via removeWhere" {

        val source = ListOfDoubles(values = listOf(1.1, 2.2, 3.3))

        val result = source.mutate {
            values.removeWhere { this > 1.1 }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.values shouldNotBe result.values
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf(1.1)
            }
        }
    }

    "Mutating a list of doubles by removing elements via retainWhere" {

        val source = ListOfDoubles(values = listOf(1.1, 2.2, 3.3))

        val result = source.mutate {
            values.retainWhere { this > 1.1 }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.values shouldNotBe result.values
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf(2.2, 3.3)
            }
        }
    }

    ////  List of strings  //////////////////////////////////////////////////////////////////

    "Mutating a list of strings by removing elements via removeWhere" {

        val source = ListOfStrings(values = listOf("Berlin", "Leipzig"))

        val result = source.mutate {
            values.removeWhere { startsWith("L") }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.values shouldNotBe result.values
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf("Berlin")
            }
        }
    }

    "Mutating a list of strings by removing elements via retainWhere" {

        val source = ListOfStrings(values = listOf("Berlin", "Leipzig"))

        val result = source.mutate {
            values.retainWhere { startsWith("L") }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBe result
                source.values shouldNotBe result.values
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf("Leipzig")
            }
        }
    }


})
