package de.peekandpoke.ultra.mutator.e2e

import io.kotlintest.DisplayName
import io.kotlintest.assertSoftly
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.matchers.types.shouldNotBeSameInstanceAs
import io.kotlintest.matchers.withClue
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

@DisplayName("E2E - ListOfScalarsMutationsSpec")
class ListOfScalarsMutationsSpec : StringSpec({

    ////  List of bools  //////////////////////////////////////////////////////////////////////

    "Mutating a list of bools by removing elements via removeWhere" {

        val data = listOf(true, false, true)
        val dataCopy = data.toList()

        val source = ListOfBools(values = data)

        val result = source.mutate {
            values.removeWhere { this }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.values shouldBe dataCopy
                source.values shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf(false)
            }
        }
    }

    "Mutating a list of bools by removing elements via retainWhere" {

        val data = listOf(true, false, true)
        val dataCopy = data.toList()

        val source = ListOfBools(values = data)

        val result = source.mutate {
            values.retainWhere { this }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.values shouldBe dataCopy
                source.values shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf(true, true)
            }
        }
    }

    ////  List of chars  //////////////////////////////////////////////////////////////////////

    "Mutating a list of chars by removing elements via removeWhere" {

        val data = listOf('A', 'B', 'C')
        val dataCopy = data.toList()

        val source = ListOfChars(values = data)

        val result = source.mutate {
            values.removeWhere { this > 'A' }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.values shouldBe dataCopy
                source.values shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf('A')
            }
        }
    }

    "Mutating a list of chars by removing elements via retainWhere" {

        val data = listOf('A', 'B', 'C')
        val dataCopy = data.toList()

        val source = ListOfChars(values = data)

        val result = source.mutate {
            values.retainWhere { this > 'A' }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.values shouldBe dataCopy
                source.values shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf('B', 'C')
            }
        }
    }

    ////  List of bytes  //////////////////////////////////////////////////////////////////////

    "Mutating a list of bytes by removing elements via removeWhere" {

        val data = listOf<Byte>(1, 2, 3)
        val dataCopy = data.toList()

        val source = ListOfBytes(values = data)

        val result = source.mutate {
            values.removeWhere { this > 1 }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.values shouldBe dataCopy
                source.values shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf<Byte>(1)
            }
        }
    }

    "Mutating a list of bytes by removing elements via retainWhere" {

        val data = listOf<Byte>(1, 2, 3)
        val dataCopy = data.toList()

        val source = ListOfBytes(values = data)

        val result = source.mutate {
            values.retainWhere { this > 1 }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.values shouldBe dataCopy
                source.values shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf<Byte>(2, 3)
            }
        }
    }

    ////  List of shorts  //////////////////////////////////////////////////////////////////////

    "Mutating a list of shorts by removing elements via removeWhere" {

        val data = listOf<Short>(1, 2, 3)
        val dataCopy = data.toList()

        val source = ListOfShorts(values = data)

        val result = source.mutate {
            values.removeWhere { this > 1 }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.values shouldBe dataCopy
                source.values shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf<Short>(1)
            }
        }
    }

    "Mutating a list of shorts by removing elements via retainWhere" {

        val data = listOf<Short>(1, 2, 3)
        val dataCopy = data.toList()

        val source = ListOfShorts(values = data)

        val result = source.mutate {
            values.retainWhere { this > 1 }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.values shouldBe dataCopy
                source.values shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf<Short>(2, 3)
            }
        }
    }

    ////  List of ints  //////////////////////////////////////////////////////////////////////

    "Mutating a list of ints by removing elements via removeWhere" {

        val data = listOf(1, 2, 3)
        val dataCopy = data.toList()

        val source = ListOfInts(values = data)

        val result = source.mutate {
            values.removeWhere { this > 1 }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.values shouldBe dataCopy
                source.values shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf(1)
            }
        }
    }

    "Mutating a list of ints by removing elements via retainWhere" {

        val data = listOf(1, 2, 3)
        val dataCopy = data.toList()

        val source = ListOfInts(values = data)

        val result = source.mutate {
            values.retainWhere { this > 1 }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.values shouldBe dataCopy
                source.values shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf(2, 3)
            }
        }
    }

    ////  List of longs  //////////////////////////////////////////////////////////////////////

    "Mutating a list of longs by removing elements via removeWhere" {

        val data = listOf<Long>(1, 2, 3)
        val dataCopy = data.toList()

        val source = ListOfLongs(values = data)

        val result = source.mutate {
            values.removeWhere { this > 1 }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.values shouldBe dataCopy
                source.values shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf<Long>(1)
            }
        }
    }

    "Mutating a list of longs by removing elements via retainWhere" {

        val data = listOf<Long>(1, 2, 3)
        val dataCopy = data.toList()

        val source = ListOfLongs(values = data)

        val result = source.mutate {
            values.retainWhere { this > 1 }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.values shouldBe dataCopy
                source.values shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf<Long>(2, 3)
            }
        }
    }

    ////  List of floats  //////////////////////////////////////////////////////////////////////

    "Mutating a list of floats by removing elements via removeWhere" {

        val data = listOf(1.1f, 2.2f, 3.3f)
        val dataCopy = data.toList()

        val source = ListOfFloats(values = data)

        val result = source.mutate {
            values.removeWhere { this > 1.1f }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.values shouldBe dataCopy
                source.values shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf(1.1f)
            }
        }
    }

    "Mutating a list of floats by removing elements via retainWhere" {

        val data = listOf(1.1f, 2.2f, 3.3f)
        val dataCopy = data.toList()

        val source = ListOfFloats(values = data)

        val result = source.mutate {
            values.retainWhere { this > 1.1f }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.values shouldBe dataCopy
                source.values shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf(2.2f, 3.3f)
            }
        }
    }

    ////  List of doubles  //////////////////////////////////////////////////////////////////////

    "Mutating a list of doubles by removing elements via removeWhere" {

        val data = listOf(1.1, 2.2, 3.3)
        val dataCopy = data.toList()

        val source = ListOfDoubles(values = data)

        val result = source.mutate {
            values.removeWhere { this > 1.1 }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.values shouldBe dataCopy
                source.values shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf(1.1)
            }
        }
    }

    "Mutating a list of doubles by removing elements via retainWhere" {

        val data = listOf(1.1, 2.2, 3.3)
        val dataCopy = data.toList()

        val source = ListOfDoubles(values = data)

        val result = source.mutate {
            values.retainWhere { this > 1.1 }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.values shouldBe dataCopy
                source.values shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf(2.2, 3.3)
            }
        }
    }

    ////  List of strings  //////////////////////////////////////////////////////////////////

    "Mutating a list of strings by removing elements via removeWhere" {

        val data = listOf("Berlin", "Leipzig")
        val dataCopy = data.toList()

        val source = ListOfStrings(values = data)

        val result = source.mutate {
            values.removeWhere { startsWith("L") }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.values shouldBe dataCopy
                source.values shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf("Berlin")
            }
        }
    }

    "Mutating a list of strings by removing elements via retainWhere" {

        val data = listOf("Berlin", "Leipzig")
        val dataCopy = data.toList()

        val source = ListOfStrings(values = data)

        val result = source.mutate {
            values.retainWhere { startsWith("L") }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.values shouldBe dataCopy
                source.values shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf("Leipzig")
            }
        }
    }
})
