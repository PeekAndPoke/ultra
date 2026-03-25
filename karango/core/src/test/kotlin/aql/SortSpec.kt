package io.peekandpoke.karango.aql

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.AqlPrinter.Companion.printRawQuery

class SortSpec : StringSpec({

    "SORT ASC single field" {
        val query = FOR("x", ARRAY(1.aql, 2.aql, 3.aql)) { x ->
            SORT(x.ASC)
            RETURN(x)
        }

        query.printRawQuery() shouldBe """
            FOR `x` IN [1, 2, 3]
                SORT `x` ASC
                RETURN `x`

        """.trimIndent()
    }

    "SORT DESC single field" {
        val query = FOR("x", ARRAY(1.aql, 2.aql, 3.aql)) { x ->
            SORT(x.DESC)
            RETURN(x)
        }

        query.printRawQuery() shouldBe """
            FOR `x` IN [1, 2, 3]
                SORT `x` DESC
                RETURN `x`

        """.trimIndent()
    }

    "SORT multiple fields" {
        val query = FOR("x", ARRAY(1.aql, 2.aql, 3.aql)) { x ->
            SORT(x.ASC, x.DESC)
            RETURN(x)
        }

        query.printRawQuery() shouldBe """
            FOR `x` IN [1, 2, 3]
                SORT `x` ASC, `x` DESC
                RETURN `x`

        """.trimIndent()
    }

    "SORT with sort() helper and explicit direction" {
        val query = FOR("x", ARRAY(1.aql, 2.aql, 3.aql)) { x ->
            SORT(x, AqlSortDirection.DESC)
            RETURN(x)
        }

        query.printRawQuery() shouldBe """
            FOR `x` IN [1, 2, 3]
                SORT `x` DESC
                RETURN `x`

        """.trimIndent()
    }

    "SORT with default direction (ASC)" {
        val query = FOR("x", ARRAY(1.aql, 2.aql, 3.aql)) { x ->
            SORT(x)
            RETURN(x)
        }

        query.printRawQuery() shouldBe """
            FOR `x` IN [1, 2, 3]
                SORT `x` ASC
                RETURN `x`

        """.trimIndent()
    }
})
