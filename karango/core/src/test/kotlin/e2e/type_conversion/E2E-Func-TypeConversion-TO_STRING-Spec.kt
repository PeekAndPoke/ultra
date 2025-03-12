package de.peekandpoke.karango.e2e.type_conversion

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.TO_STRING
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.E2ePerson
import de.peekandpoke.karango.e2e.karangoDriver
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-TypeConversion-TO_STRING-Spec` : StringSpec({

    "TO_STRING conversion of 'null' directly" {

        val result = karangoDriver.query {
            RETURN(
                TO_STRING(null.aql())
            )
        }

        result.toList() shouldBe listOf("")

        val result2 = karangoDriver.query {
            RETURN(
                TO_STRING(null.aql)
            )
        }

        result2.toList() shouldBe listOf("")
    }

    "TO_STRING conversion of 'null' from LET" {

        val result = karangoDriver.query {
            val l = LET("l", null)
            RETURN(
                TO_STRING(l)
            )
        }

        result.toList() shouldBe listOf("")

        val result2 = karangoDriver.query {
            val l = LET("l", null.aql())
            RETURN(
                TO_STRING(l)
            )
        }

        result2.toList() shouldBe listOf("")

        val result3 = karangoDriver.query {
            val l = LET("l", null.aql)
            RETURN(
                TO_STRING(l)
            )
        }

        result3.toList() shouldBe listOf("")
    }

    val cases = listOf(
        row("TO_STRING(false)", false, "false"),
        row("TO_STRING(true)", true, "true"),

        row("TO_STRING(0)", 0, "0"),
        row("TO_STRING(1)", 1, "1"),
        row("TO_STRING(-1)", -1, "-1"),

        row("TO_STRING(0.0)", 0.0, "0"),
        row("TO_STRING(0.1)", 0.1, "0.1"),
        row("TO_STRING(-0.1)", -0.1, "-0.1"),

        row("TO_STRING(\"\") empty string", "", ""),
        row("TO_STRING(\"a\") none empty string", "a", "a"),

        row("TO_STRING([]) empty list", listOf<Int>(), "[]"),
        row("TO_STRING([0]) none empty list", listOf(0), "[0]"),
        row("TO_STRING([1]) none empty list", listOf(1), "[1]"),
        row("TO_STRING([0, 0]) none empty list", listOf(0, 0), "[0,0]"),
        row("TO_STRING([0, 1]) none empty list", listOf(0, 1), "[0,1]"),
        row("TO_STRING([1, 1]) none empty list", listOf(1, 1), "[1,1]"),
        row("TO_STRING([1, 0]) none empty list", listOf(1, 0), "[1,0]"),
        row("TO_STRING([1, [2, 3]]) none empty list", listOf(1, listOf(2, 3)), "[1,[2,3]]"),
        row("TO_STRING(['x']) none empty list", listOf("x"), """["x"]"""),
        row("TO_STRING(['x', 'x']) none empty list", listOf("x", "x"), """["x","x"]"""),

        row("TO_STRING(object)", E2ePerson("a", 1), """{"name":"a","age":1}"""),
        row(
            "TO_STRING([object]) list with one objects",
            listOf(E2ePerson("a", 1)),
            """[{"name":"a","age":1}]"""
        ),
        row(
            "TO_STRING([object, object]) list with two objects",
            listOf(E2ePerson("a", 1), E2ePerson("b", 2)),
            """[{"name":"a","age":1},{"name":"b","age":2}]"""
        )
    )

    for ((description, expression, expected) in cases) {

        "$description - return directly" {

            val result = karangoDriver.query {
                RETURN(
                    TO_STRING(expression.aql())
                )
            }

            val result2 = karangoDriver.query {
                RETURN(
                    TO_STRING(expression.aql)
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
                    TO_STRING(l)
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
                    TO_STRING(l)
                )
            }

            val result2 = karangoDriver.query {
                val l = LET("l", expression.aql)

                RETURN(
                    TO_STRING(l)
                )
            }

            val result3 = karangoDriver.query {
                val l = LET("l", expression.aql)

                RETURN(
                    l.TO_STRING
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
