package de.peekandpoke.ultra.mutator.e2e

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.DisplayName
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs

@DisplayName("E2E - ListOfScalarNullablesMutationsSpec")
class ListOfScalarNullablesMutationsSpec : StringSpec({

    //  List of nullable ints  //////////////////////////////////////////////////////////////////////

    "Mutating a list of nullable ints by removing elements via removeWhere" {

        val data = listOf(1, 2, null, 3)
        val dataCopy = data.toList()

        val source = ListOfNullableInts(values = data)

        val result = source.mutate {
            values.removeWhere { this != null && this > 1 }
        }

        assertSoftly {

            withClue("Source object must NOT be modified") {
                source shouldNotBeSameInstanceAs result
                source.values shouldBe dataCopy
                source.values shouldBeSameInstanceAs data
            }

            withClue("Result must be modified properly") {
                result.values shouldBe listOf(1, null)
            }
        }
    }

    "Mutating a list of nullable ints by removing elements via retainWhere" {

        val data = listOf(1, 2, null, 3)
        val dataCopy = data.toList()

        val source = ListOfNullableInts(values = data)

        val result = source.mutate {
            values.retainWhere { this != null && this > 1 }
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
})
