package de.peekandpoke.karango.e2e.type_conversion

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.TO_ARRAY
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.E2ePerson
import de.peekandpoke.karango.e2e.karangoDriver
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-TypeConversion-TO_ARRAY-Spec` : StringSpec({

    "TO_ARRAY conversion of 'null' directly" {

        val result = karangoDriver.query {
            RETURN(
                TO_ARRAY(null.aql())
            )
        }

        result.toList() shouldBe listOf(listOf<Any>())

        val result2 = karangoDriver.query {
            RETURN(
                TO_ARRAY(null.aql)
            )
        }

        result2.toList() shouldBe listOf(listOf<Any>())
    }

    "TO_ARRAY conversion of 'null' from LET" {

        val result = karangoDriver.query {
            val l = LET("l", null)
            RETURN(
                TO_ARRAY(l)
            )
        }

        result.toList() shouldBe listOf(listOf<Any>())

        val result2 = karangoDriver.query {
            val l = LET("l", null.aql())
            RETURN(
                TO_ARRAY(l)
            )
        }

        result2.toList() shouldBe listOf(listOf<Any>())

        val result3 = karangoDriver.query {
            val l = LET("l", null.aql)
            RETURN(
                TO_ARRAY(l)
            )
        }

        result3.toList() shouldBe listOf(listOf<Any>())
    }


    val cases = listOf(
        row("TO_ARRAY(false)", false, listOf(false)),
        row("TO_ARRAY(true)", true, listOf(true)),

        row("TO_ARRAY(0)", 0, listOf(0L)),
        row("TO_ARRAY(1)", 1, listOf(1L)),
        row("TO_ARRAY(-1)", -1, listOf(-1L)),

        row("TO_ARRAY(0.0)", 0.0, listOf(0.0)),
        row("TO_ARRAY(0.1)", 0.1, listOf(0.1)),
        row("TO_ARRAY(-0.1)", -0.1, listOf(-0.1)),

        row("TO_ARRAY(\"\") empty string", "", listOf("")),
        row("TO_ARRAY(\"a\") none empty string", "a", listOf("a")),

        row("TO_ARRAY([]) empty list", listOf<Int>(), listOf<Int>()),
        row("TO_ARRAY([0]) none empty list", listOf(0), listOf(0L)),
        row("TO_ARRAY([1]) none empty list", listOf(1), listOf(1L)),
        row("TO_ARRAY([0, 0]) none empty list", listOf(0, 0), listOf(0L, 0L)),
        row("TO_ARRAY([0, 1]) none empty list", listOf(0, 1), listOf(0L, 1L)),
        row("TO_ARRAY([1, 1]) none empty list", listOf(1, 1), listOf(1L, 1L)),
        row("TO_ARRAY([1, 0]) none empty list", listOf(1, 0), listOf(1L, 0L)),
        row("TO_ARRAY([1, [2, 3]]) none empty list", listOf(1, listOf(2, 3)), listOf(1L, listOf(2L, 3L))),
        row("TO_ARRAY(['x']) none empty list", listOf("x"), listOf("x")),
        row("TO_ARRAY(['x', 'x']) none empty list", listOf("x", "x"), listOf("x", "x")),

        row("TO_ARRAY(object)", E2ePerson("a", 1), listOf("a", 1L)),
        row(
            "TO_ARRAY([object]) list with one objects",
            listOf(E2ePerson("a", 1)),
            listOf(mapOf("name" to "a", "age" to 1L))
        ),
        row(
            "TO_ARRAY([object, object]) list with two objects",
            listOf(E2ePerson("a", 1), E2ePerson("b", 2)),
            listOf(mapOf("name" to "a", "age" to 1L), mapOf("name" to "b", "age" to 2L))
        )
    )

    for ((description, expression, expected) in cases) {

        "$description - return directly" {

            val result = karangoDriver.query {
                RETURN(
                    TO_ARRAY(expression.aql())
                )
            }

            val result2 = karangoDriver.query {
                RETURN(
                    TO_ARRAY(expression.aql)
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

                RETURN(
                    TO_ARRAY(l)
                )
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
                    TO_ARRAY(l)
                )
            }

            val result2 = karangoDriver.query {
                val l = LET("l", expression.aql)

                RETURN(
                    TO_ARRAY(l)
                )
            }

            val result3 = karangoDriver.query {
                val l = LET("l", expression.aql)

                RETURN(
                    l.TO_ARRAY
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
