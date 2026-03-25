package io.peekandpoke.karango.aql

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.AqlPrinter.Companion.printRawQuery

class LimitSpec : StringSpec({

    "LIMIT(n)" {
        val query = FOR("x", ARRAY(1.aql, 2.aql, 3.aql)) { x ->
            LIMIT(10)
            RETURN(x)
        }

        query.printRawQuery() shouldBe """
            FOR `x` IN [1, 2, 3]
                LIMIT 0, 10
                RETURN `x`

        """.trimIndent()
    }

    "LIMIT(offset, limit)" {
        val query = FOR("x", ARRAY(1.aql, 2.aql, 3.aql)) { x ->
            LIMIT(5, 10)
            RETURN(x)
        }

        query.printRawQuery() shouldBe """
            FOR `x` IN [1, 2, 3]
                LIMIT 5, 10
                RETURN `x`

        """.trimIndent()
    }

    "PAGE defaults to page 1 with 20 epp" {
        val query = FOR("x", ARRAY(1.aql, 2.aql, 3.aql)) { x ->
            PAGE()
            RETURN(x)
        }

        query.printRawQuery() shouldBe """
            FOR `x` IN [1, 2, 3]
                LIMIT 0, 20
                RETURN `x`

        """.trimIndent()
    }

    "PAGE(page=2, epp=10) computes correct offset" {
        val query = FOR("x", ARRAY(1.aql, 2.aql, 3.aql)) { x ->
            PAGE(page = 2, epp = 10)
            RETURN(x)
        }

        query.printRawQuery() shouldBe """
            FOR `x` IN [1, 2, 3]
                LIMIT 10, 10
                RETURN `x`

        """.trimIndent()
    }

    "PAGE(page=3, epp=5) computes correct offset" {
        val query = FOR("x", ARRAY(1.aql, 2.aql, 3.aql)) { x ->
            PAGE(page = 3, epp = 5)
            RETURN(x)
        }

        query.printRawQuery() shouldBe """
            FOR `x` IN [1, 2, 3]
                LIMIT 10, 5
                RETURN `x`

        """.trimIndent()
    }

    "PAGE with page 0 clamps to offset 0" {
        val query = FOR("x", ARRAY(1.aql, 2.aql, 3.aql)) { x ->
            PAGE(page = 0, epp = 10)
            RETURN(x)
        }

        query.printRawQuery() shouldBe """
            FOR `x` IN [1, 2, 3]
                LIMIT 0, 10
                RETURN `x`

        """.trimIndent()
    }

    "SKIP(n)" {
        val query = FOR("x", ARRAY(1.aql, 2.aql, 3.aql)) { x ->
            SKIP(5)
            RETURN(x)
        }

        query.printRawQuery() shouldBe """
            FOR `x` IN [1, 2, 3]
                LIMIT 5, null
                RETURN `x`

        """.trimIndent()
    }
})
