package de.peekandpoke.karango.e2e.type_conversion

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.TO_BOOL
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.E2ePerson
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class E2E_Func_TO_BOOL_Spec : StringSpec({

    "TO_BOOL conversion of 'null' directly" {

        val result = karangoDriver.query {
            RETURN(
                TO_BOOL(null.aql())
            )
        }

        result.toList() shouldBe listOf(false)

        val result2 = karangoDriver.query {
            RETURN(
                TO_BOOL(null.aql)
            )
        }

        result2.toList() shouldBe listOf(false)
    }

    "TO_BOOL conversion of 'null' from LET" {

        val result = karangoDriver.query {
            val l = LET("l", null)

            RETURN(
                TO_BOOL(l)
            )
        }

        result.toList() shouldBe listOf(false)

        val result2 = karangoDriver.query {
            val l = LET("l", null.aql())

            RETURN(
                TO_BOOL(l)
            )
        }

        result2.toList() shouldBe listOf(false)

        val result3 = karangoDriver.query {
            val l = LET("l", null.aql)

            RETURN(
                TO_BOOL(l)
            )
        }

        result3.toList() shouldBe listOf(false)
    }

    val cases = listOf(
        tuple("TO_BOOL(false)", false, false),
        tuple("TO_BOOL(true)", true, true),

        tuple("TO_BOOL(0)", 0, false),
        tuple("TO_BOOL(1)", 1, true),
        tuple("TO_BOOL(-1)", -1, true),

        tuple("TO_BOOL(0.0)", 0.0, false),
        tuple("TO_BOOL(0.1)", 0.1, true),
        tuple("TO_BOOL(-0.1)", -0.1, true),

        tuple("TO_BOOL(\"\") empty string", "", false),
        tuple("TO_BOOL(\"a\") none empty string", "a", true),

        tuple("TO_BOOL([]) empty list", listOf<Int>(), true),
        tuple("TO_BOOL([0]) none empty list", listOf(0), true),
        tuple("TO_BOOL([1]) none empty list", listOf(1), true),
        tuple("TO_BOOL([0, 0]) none empty list", listOf(0, 0), true),
        tuple("TO_BOOL([1, 1]) none empty list", listOf(1, 1), true),
        tuple("TO_BOOL(['x']) none empty list", listOf("x"), true),
        tuple("TO_BOOL(['x', 'x']) none empty list", listOf("x", "x"), true),

        tuple("TO_BOOL(object)", E2ePerson("a", 1), true),
        tuple("TO_BOOL([object]) list with one objects", listOf(E2ePerson("a", 1)), true),
        tuple("TO_BOOL([object, object]) list with two objects", listOf(E2ePerson("a", 1), E2ePerson("b", 2)), true)
    )

    for ((description, expression, expected) in cases) {

        "$description - return directly" {

            val result = karangoDriver.query {
                RETURN(
                    TO_BOOL(expression.aql())
                )
            }

            val result2 = karangoDriver.query {
                RETURN(
                    TO_BOOL(expression.aql)
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

                RETURN(TO_BOOL(l))
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
                    TO_BOOL(l)
                )
            }

            val result2 = karangoDriver.query {
                val l = LET("l", expression.aql)

                RETURN(
                    TO_BOOL(l)
                )
            }

            val result3 = karangoDriver.query {
                val l = LET("l", expression.aql)

                RETURN(
                    l.TO_BOOL
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
