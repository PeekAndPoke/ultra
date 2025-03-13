package de.peekandpoke.karango

import de.peekandpoke.karango.aql.ALL
import de.peekandpoke.karango.aql.ANY
import de.peekandpoke.karango.aql.EQ
import de.peekandpoke.karango.aql.FOR
import de.peekandpoke.karango.aql.GT
import de.peekandpoke.karango.aql.GTE
import de.peekandpoke.karango.aql.IN
import de.peekandpoke.karango.aql.LIKE
import de.peekandpoke.karango.aql.LT
import de.peekandpoke.karango.aql.LTE
import de.peekandpoke.karango.aql.NE
import de.peekandpoke.karango.aql.NONE
import de.peekandpoke.karango.aql.NOT_IN
import de.peekandpoke.karango.aql.REGEX
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.contract
import de.peekandpoke.karango.aql.expand
import de.peekandpoke.karango.e2e.database
import de.peekandpoke.karango.testdomain.addresses
import de.peekandpoke.karango.testdomain.authors
import de.peekandpoke.karango.testdomain.books
import de.peekandpoke.karango.testdomain.firstName
import de.peekandpoke.karango.testdomain.name
import de.peekandpoke.karango.testdomain.street
import de.peekandpoke.karango.testdomain.testPersons
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class QueryBuilderSpec : StringSpec() {

    init {
        "Filter operators must be applied correctly" {

            val query = buildQuery {
                FOR("iter", database.testPersons) {
                    FILTER(it.name EQ "V_EQ")
                    FILTER(it.name NE "V_NE")
                    FILTER(it.name GT "V_GT")
                    FILTER(it.name GTE "V_GTE")
                    FILTER(it.name LT "V_LT")
                    FILTER(it.name LTE "V_LTE")
                    FILTER(it.name IN listOf("V_IN"))
                    FILTER(it.name NOT_IN listOf("V_NOT_IN"))
                    FILTER(it.name LIKE "V_LIKE")
                    FILTER(it.name REGEX "V_REGEX")
                    RETURN(it)
                }
            }

            assertSoftly {
                query.query.shouldContain("FOR `iter` IN `test-persons`")
                query.query.shouldContain("FILTER `iter`.`name` == @v_1")
                query.query.shouldContain("FILTER `iter`.`name` != @v_2")
                query.query.shouldContain("FILTER `iter`.`name` > @v_3")
                query.query.shouldContain("FILTER `iter`.`name` >= @v_4")
                query.query.shouldContain("FILTER `iter`.`name` < @v_5")
                query.query.shouldContain("FILTER `iter`.`name` <= @v_6")
                query.query.shouldContain("FILTER `iter`.`name` IN @v_7")
                query.query.shouldContain("FILTER `iter`.`name` NOT IN @v_8")
                query.query.shouldContain("FILTER `iter`.`name` LIKE @v_9")
                query.query.shouldContain("FILTER `iter`.`name` =~ @v_10")
                query.query.shouldContain("RETURN `iter`")

                query.query.shouldBe(
                    """ |FOR `iter` IN `test-persons`
                    |    FILTER `iter`.`name` == @v_1
                    |    FILTER `iter`.`name` != @v_2
                    |    FILTER `iter`.`name` > @v_3
                    |    FILTER `iter`.`name` >= @v_4
                    |    FILTER `iter`.`name` < @v_5
                    |    FILTER `iter`.`name` <= @v_6
                    |    FILTER `iter`.`name` IN @v_7
                    |    FILTER `iter`.`name` NOT IN @v_8
                    |    FILTER `iter`.`name` LIKE @v_9
                    |    FILTER `iter`.`name` =~ @v_10
                    |    RETURN `iter`
                    |
                """.trimMargin()
                )

                query.vars.shouldBe(
                    mapOf(
                        "v_1" to "V_EQ",
                        "v_2" to "V_NE",
                        "v_3" to "V_GT",
                        "v_4" to "V_GTE",
                        "v_5" to "V_LT",
                        "v_6" to "V_LTE",
                        "v_7" to listOf("V_IN"),
                        "v_8" to listOf("V_NOT_IN"),
                        "v_9" to "V_LIKE",
                        "v_10" to "V_REGEX"
                    )
                )
            }
        }

        "Iterator name must be taken from parameter name of FOR loop" {

            val query = buildQuery {
                FOR(database.testPersons) { iterator ->
                    FILTER(iterator.addresses.expand().street ALL EQ("street"))
                    RETURN(iterator)
                }
            }

            withClue("Parameter name will be p2_<HASH> ... using the hashcode of the lambda function") {
                query.query.shouldContain("FOR `p2_.*` IN `test-persons`".toRegex())
            }
        }

        "Property path must be rendered correctly" {

            val query = buildQuery {
                FOR("p", database.testPersons) {
                    FILTER(it.addresses.expand().street ALL EQ("street"))
                    RETURN(it)
                }
            }

            query.query.shouldBe(
                """ |FOR `p` IN `test-persons`
                |    FILTER `p`.`addresses`[*].`street` ALL == @v_1
                |    RETURN `p`
                |
            """.trimMargin()
            )
        }

        "Property path with Array expansion [*] and contraction [**] must be rendered correctly" {

            val query = buildQuery {
                FOR("person", database.testPersons) {
                    FILTER(it.books.expand().authors.expand().firstName.contract() ALL EQ("street"))
                    RETURN(it)
                }
            }

            query.query.shouldBe(
                """ |FOR `person` IN `test-persons`
                |    FILTER `person`.`books`[*].`authors`[*].`firstName`[**] ALL == @v_1
                |    RETURN `person`
                |
            """.trimMargin()
            )
        }

        "Array operators ANY, ALL, NONE must be rendered correctly" {

            val query = buildQuery {
                FOR("person", database.testPersons) {
                    FILTER(it.addresses.expand().street ANY EQ("str"))
                    FILTER(it.addresses.expand().street ALL NE("str"))
                    FILTER(it.addresses.expand().street NONE IN(listOf("list")))
                    RETURN(it)
                }
            }

            query.query.shouldBe(
                """ |FOR `person` IN `test-persons`
                |    FILTER `person`.`addresses`[*].`street` ANY == @v_1
                |    FILTER `person`.`addresses`[*].`street` ALL != @v_1
                |    FILTER `person`.`addresses`[*].`street` NONE IN @v_2
                |    RETURN `person`
                |
            """.trimMargin()
            )

            query.vars shouldBe mapOf(
                "v_1" to "str",
                "v_2" to listOf("list"),
            )
        }
    }
}
