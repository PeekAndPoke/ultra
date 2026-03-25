package io.peekandpoke.karango.aql

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.AqlPrinter.Companion.printRawQuery

class CollectSpec : StringSpec({

    "COLLECT_WITH COUNT" {
        val query = FOR("x", ARRAY(1.aql, 2.aql, 3.aql)) { _ ->
            val count = COLLECT_WITH(COUNT, "count")
            RETURN(count)
        }

        query.printRawQuery() shouldBe """
            FOR `x` IN [1, 2, 3]
                COLLECT WITH COUNT INTO count
                RETURN `count`

        """.trimIndent()
    }

    "COLLECT into assignee" {
        val query = FOR("x", ARRAY(1.aql, 2.aql, 3.aql)) { x ->
            val group = COLLECT("group", x)
            RETURN(group.assignee)
        }

        query.printRawQuery() shouldBe """
            FOR `x` IN [1, 2, 3]
                COLLECT `group` = `x`
                RETURN `group`

        """.trimIndent()
    }

    "COLLECT_INTO with into variable" {
        val query = FOR("x", ARRAY(1.aql, 2.aql, 3.aql)) { x ->
            val collect = COLLECT_INTO("group", x, "items")
            RETURN(collect.into)
        }

        query.printRawQuery() shouldBe """
            FOR `x` IN [1, 2, 3]
                COLLECT `group` = `x` INTO `items`
                RETURN `items`

        """.trimIndent()
    }

    "COLLECT_AGGREGATE" {
        val query = FOR("x", ARRAY(1.aql, 2.aql, 3.aql)) { x ->
            val collect = COLLECT_AGGREGATE("group", x, "total", x)
            RETURN(collect.aggregate)
        }

        query.printRawQuery() shouldBe """
            FOR `x` IN [1, 2, 3]
                COLLECT `group` = `x` AGGREGATE `total` = `x`
                RETURN `total`

        """.trimIndent()
    }
})
