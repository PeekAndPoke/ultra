package io.peekandpoke.karango.aql

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.AqlPrinter.Companion.printRawQuery

class ReturnVariantsSpec : StringSpec({

    "RETURN prints correctly" {
        val query = FOR("x", ARRAY(1.aql, 2.aql)) { x ->
            RETURN(x)
        }

        query.printRawQuery() shouldBe """
            FOR `x` IN [1, 2]
                RETURN `x`

        """.trimIndent()
    }

    "RETURN_DISTINCT prints correctly" {
        val query = FOR("x", ARRAY(1.aql, 2.aql, 1.aql)) { x ->
            RETURN_DISTINCT(x)
        }

        query.printRawQuery() shouldBe """
            FOR `x` IN [1, 2, 1]
                RETURN DISTINCT `x`

        """.trimIndent()
    }

    "RETURN_COUNT prints correctly" {
        val query = FOR("x", ARRAY(1.aql, 2.aql, 3.aql)) { _ ->
            RETURN_COUNT()
        }

        query.printRawQuery() shouldBe """
            FOR `x` IN [1, 2, 3]
                COLLECT WITH COUNT INTO count
                RETURN `count`

        """.trimIndent()
    }

    "RETURN_COUNT with custom variable name" {
        val query = FOR("x", ARRAY(1.aql, 2.aql, 3.aql)) { _ ->
            RETURN_COUNT("total")
        }

        query.printRawQuery() shouldBe """
            FOR `x` IN [1, 2, 3]
                COLLECT WITH COUNT INTO total
                RETURN `total`

        """.trimIndent()
    }

    "FILTER_ANY prints correctly" {
        val query = FOR("x", ARRAY(1.aql, 2.aql, 3.aql)) { x ->
            FILTER_ANY(x EQ 1, x EQ 3)
            RETURN(x)
        }

        query.printRawQuery() shouldBe """
            FOR `x` IN [1, 2, 3]
                FILTER (`x` == 1) OR (`x` == 3)
                RETURN `x`

        """.trimIndent()
    }
})
