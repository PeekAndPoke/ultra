package de.peekandpoke.karango.e2e.type_conversion

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.TO_NUMBER
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.E2ePerson
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class E2E_Func_TO_NUMBER_Spec : StringSpec({

    "TO_NUMBER conversion of 'null' directly" {

        val result = karangoDriver.query {
            RETURN(
                TO_NUMBER(null.aql())
            )
        }

        result.toList() shouldBe listOf(0.0)

        val result2 = karangoDriver.query {
            RETURN(
                TO_NUMBER(null.aql)
            )
        }

        result2.toList() shouldBe listOf(0.0)
    }

    "TO_NUMBER conversion of 'null' from LET" {

        val result = karangoDriver.query {
            val l = LET("l", null)
            RETURN(
                TO_NUMBER(l)
            )
        }

        result.toList() shouldBe listOf(0.0)

        val result2 = karangoDriver.query {
            val l = LET("l", null.aql())
            RETURN(
                TO_NUMBER(l)
            )
        }

        result2.toList() shouldBe listOf(0.0)

        val result3 = karangoDriver.query {
            val l = LET("l", null.aql)
            RETURN(
                TO_NUMBER(l)
            )
        }

        result3.toList() shouldBe listOf(0.0)
    }

    val cases = listOf(
        tuple("TO_NUMBER(false)", false, 0.0),
        tuple("TO_NUMBER(true)", true, 1.0),

//        tuple("TO_NUMBER(0)", 0, 0.0),
//        tuple("TO_NUMBER(1)", 1, 1.0),
//        tuple("TO_NUMBER(-1)", -1, -1.0),

        tuple("TO_NUMBER(0.0)", 0.0, 0.0),
        tuple("TO_NUMBER(0.1)", 0.1, 0.1),
        tuple("TO_NUMBER(-0.1)", -0.1, -0.1),

        tuple("TO_NUMBER(\"\") empty string", "", 0.0),
        tuple("TO_NUMBER(\"a\") none empty string", "a", 0L),

        tuple("TO_NUMBER([]) empty list", listOf<Int>(), 0.0),
        tuple("TO_NUMBER([0]) none empty list", listOf(0), 0.0),
        tuple("TO_NUMBER([1]) none empty list", listOf(1), 1.0),
        tuple("TO_NUMBER([0, 0]) none empty list", listOf(0, 0), 0L),
        tuple("TO_NUMBER([0, 1]) none empty list", listOf(0, 1), 0L),
        tuple("TO_NUMBER([1, 1]) none empty list", listOf(1, 1), 0L),
        tuple("TO_NUMBER([1, 0]) none empty list", listOf(1, 0), 0L),
        tuple("TO_NUMBER([1, [2, 3]]) none empty list", listOf(1, listOf(2, 3)), 0L),
        tuple("TO_NUMBER(['x']) none empty list", listOf("x"), 0L),
        tuple("TO_NUMBER(['x', 'x']) none empty list", listOf("x", "x"), 0L),

        tuple("TO_NUMBER(object)", E2ePerson("a", 1), 0L),
        tuple("TO_NUMBER([object]) list with one objects", listOf(E2ePerson("a", 1)), 0L),
        tuple("TO_NUMBER([object, object]) list with two objects", listOf(E2ePerson("a", 1), E2ePerson("b", 2)), 0L)
    )

    for ((description, expression, expected) in cases) {

        "$description - return directly" {

            val result = karangoDriver.query {
                RETURN(
                    TO_NUMBER(expression.aql())
                )
            }

            val result2 = karangoDriver.query {
                RETURN(
                    TO_NUMBER(expression.aql)
                )
            }

            withClue(result.query.query + "\n\n" + result.query.vars + "\n\n") {
                result.toList() shouldBe listOf(expected)
                result2.toList() shouldBe listOf(expected)
            }
        }
    }

    for ((description, expression, expected) in cases) {

        "$description - return from LET" {

            val result = karangoDriver.query {
                val l = LET("l", expression)

                RETURN(TO_NUMBER(l))
            }

            withClue(result.query.query + "\n\n" + result.query.vars + "\n\n") {
                result.toList() shouldBe listOf(expected)
            }
        }
    }

    for ((description, expression, expected) in cases) {

        "$description - return from LET Expression" {

            val result = karangoDriver.query {
                val l = LET("l", expression.aql())

                RETURN(
                    TO_NUMBER(l)
                )
            }

            val result2 = karangoDriver.query {
                val l = LET("l", expression.aql)

                RETURN(
                    TO_NUMBER(l)
                )
            }

            val result3 = karangoDriver.query {
                val l = LET("l", expression.aql)

                RETURN(
                    l.TO_NUMBER
                )
            }

            withClue(result.query.query + "\n\n" + result.query.vars + "\n\n") {
                result.toList() shouldBe listOf(expected)
                result2.toList() shouldBe listOf(expected)
                result3.toList() shouldBe listOf(expected)
            }
        }
    }
})
