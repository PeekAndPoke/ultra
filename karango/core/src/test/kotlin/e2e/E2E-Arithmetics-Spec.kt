package de.peekandpoke.karango.e2e

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.div
import de.peekandpoke.karango.aql.minus
import de.peekandpoke.karango.aql.plus
import de.peekandpoke.karango.aql.rem
import de.peekandpoke.karango.aql.times
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Arithmetics-Spec` : StringSpec({

    "Multiple arithmetic operations at once" {

        val result = karangoDriver.query {
            val a = LET("a", 10)
            val b = LET("b", 20)
            val c = LET("c", 2)
            val d = LET("d", 3)
            RETURN((a + b) / (c * d))
        }

        assertSoftly {

            withClue("AQL-Query") {
                result.query.query shouldBe """
                    |LET `l_a` = (@l_a_1)
                    |LET `l_b` = (@l_b_2)
                    |LET `l_c` = (@l_c_3)
                    |LET `l_d` = (@l_d_4)
                    |RETURN ((`l_a` + `l_b`) / (`l_c` * `l_d`))
                    |
                """.trimMargin()
            }

            withClue("AQL-vars") {
                result.query.vars shouldBe mapOf("l_a_1" to 10, "l_b_2" to 20, "l_c_3" to 2, "l_d_4" to 3)
            }

            withClue("Query-result") {
                result.toList() shouldBe listOf(5.0)
            }
        }
    }

    "Adding two Integer expressions" {

        val result = karangoDriver.query {
            val a = LET("a", 10)
            val b = LET("b", 20)
            RETURN(a + b)
        }

        assertSoftly {

            withClue("AQL-Query") {
                result.query.query shouldBe """
                    |LET `l_a` = (@l_a_1)
                    |LET `l_b` = (@l_b_2)
                    |RETURN (`l_a` + `l_b`)
                    |
                """.trimMargin()
            }

            withClue("AQL-vars") {
                result.query.vars shouldBe mapOf("l_a_1" to 10, "l_b_2" to 20)
            }

            withClue("Query-result") {
                result.toList() shouldBe listOf(30L)
            }
        }
    }

    "Adding an Integer expression with a scalar" {

        val result = karangoDriver.query {
            val a = LET("a", 10)
            RETURN(a + 1.23)
        }

        assertSoftly {

            withClue("AQL-Query") {
                result.query.query shouldBe """
                    |LET `l_a` = (@l_a_1)
                    |RETURN (`l_a` + @v_2)
                    |
                """.trimMargin()
            }

            withClue("AQL-vars") {
                result.query.vars shouldBe mapOf("l_a_1" to 10, "v_2" to 1.23)
            }

            withClue("Query-result") {
                result.toList() shouldBe listOf(11.23)
            }
        }
    }

    "Adding a scalar with an Integer expression" {

        val result = karangoDriver.query {
            val a = LET("a", 10)
            RETURN(1.23 + a)
        }

        assertSoftly {

            withClue("AQL-Query") {
                result.query.query shouldBe """
                    |LET `l_a` = (@l_a_1)
                    |RETURN (@v_2 + `l_a`)
                    |
                """.trimMargin()
            }

            withClue("AQL-vars") {
                result.query.vars shouldBe mapOf("l_a_1" to 10, "v_2" to 1.23)
            }

            withClue("Query-result") {
                result.toList() shouldBe listOf(11.23)
            }
        }
    }

    "Subtracting two Integers expressions" {

        val result = karangoDriver.query {
            val a = LET("a", 10)
            val b = LET("b", 20)
            RETURN(a - b)
        }

        assertSoftly {

            withClue("AQL-Query") {
                result.query.query shouldBe """
                    |LET `l_a` = (@l_a_1)
                    |LET `l_b` = (@l_b_2)
                    |RETURN (`l_a` - `l_b`)
                    |
                """.trimMargin()
            }

            withClue("AQL-vars") {
                result.query.vars shouldBe mapOf("l_a_1" to 10, "l_b_2" to 20)
            }

            withClue("Query-result") {
                result.toList() shouldBe listOf(-10L)
            }
        }
    }

    "Subtracting an Integer expression with a scalar" {

        val result = karangoDriver.query {
            val a = LET("a", 10)
            RETURN(a - 1.23)
        }

        assertSoftly {

            withClue("AQL-Query") {
                result.query.query shouldBe """
                    |LET `l_a` = (@l_a_1)
                    |RETURN (`l_a` - @v_2)
                    |
                """.trimMargin()
            }

            withClue("AQL-vars") {
                result.query.vars shouldBe mapOf("l_a_1" to 10, "v_2" to 1.23)
            }

            withClue("Query-result") {
                result.toList() shouldBe listOf(8.77)
            }
        }
    }

    "Subtracting a scalar with an Integer expression" {

        val result = karangoDriver.query {
            val a = LET("a", 10)
            RETURN(1.23 - a)
        }

        assertSoftly {

            withClue("AQL-Query") {
                result.query.query shouldBe """
                    |LET `l_a` = (@l_a_1)
                    |RETURN (@v_2 - `l_a`)
                    |
                """.trimMargin()
            }

            withClue("AQL-vars") {
                result.query.vars shouldBe mapOf("l_a_1" to 10, "v_2" to 1.23)
            }

            withClue("Query-result") {
                result.toList() shouldBe listOf(-8.77)
            }
        }
    }

    "Multiplying two Integer expressions" {

        val result = karangoDriver.query {
            val a = LET("a", 10)
            val b = LET("b", 20)
            RETURN(a * b)
        }

        assertSoftly {

            withClue("AQL-Query") {
                result.query.query shouldBe """
                    |LET `l_a` = (@l_a_1)
                    |LET `l_b` = (@l_b_2)
                    |RETURN (`l_a` * `l_b`)
                    |
                """.trimMargin()
            }

            withClue("AQL-vars") {
                result.query.vars shouldBe mapOf("l_a_1" to 10, "l_b_2" to 20)
            }

            withClue("Query-result") {
                result.toList() shouldBe listOf(200L)
            }
        }
    }

    "Multiplying an Integer expression with a scalar" {

        val result = karangoDriver.query {
            val a = LET("a", 10)
            RETURN(a * 1.23)
        }

        assertSoftly {

            withClue("AQL-Query") {
                result.query.query shouldBe """
                    |LET `l_a` = (@l_a_1)
                    |RETURN (`l_a` * @v_2)
                    |
                """.trimMargin()
            }

            withClue("AQL-vars") {
                result.query.vars shouldBe mapOf("l_a_1" to 10, "v_2" to 1.23)
            }

            withClue("Query-result") {
                result.toList() shouldBe listOf(12.3)
            }
        }
    }

    "Multiplying a scalar with an Integer expression" {

        val result = karangoDriver.query {
            val a = LET("a", 10)
            RETURN(1.23 * a)
        }

        assertSoftly {

            withClue("AQL-Query") {
                result.query.query shouldBe """
                    |LET `l_a` = (@l_a_1)
                    |RETURN (@v_2 * `l_a`)
                    |
                """.trimMargin()
            }

            withClue("AQL-vars") {
                result.query.vars shouldBe mapOf("l_a_1" to 10, "v_2" to 1.23)
            }

            withClue("Query-result") {
                result.toList() shouldBe listOf(12.3)
            }
        }
    }

    "Dividing two Integer expressions" {

        val result = karangoDriver.query {
            val a = LET("a", 10)
            val b = LET("b", 20)
            RETURN(a / b)
        }

        assertSoftly {

            withClue("AQL-Query") {
                result.query.query shouldBe """
                    |LET `l_a` = (@l_a_1)
                    |LET `l_b` = (@l_b_2)
                    |RETURN (`l_a` / `l_b`)
                    |
                """.trimMargin()
            }

            withClue("AQL-vars") {
                result.query.vars shouldBe mapOf("l_a_1" to 10, "l_b_2" to 20)
            }

            withClue("Query-result") {
                result.toList() shouldBe listOf(0.5)
            }
        }
    }

    "Dividing an Integer expression with a scalar" {

        val result = karangoDriver.query {
            val a = LET("a", 10)
            RETURN(a / 2.5)
        }

        assertSoftly {

            withClue("AQL-Query") {
                result.query.query shouldBe """
                    |LET `l_a` = (@l_a_1)
                    |RETURN (`l_a` / @v_2)
                    |
                """.trimMargin()
            }

            withClue("AQL-vars") {
                result.query.vars shouldBe mapOf("l_a_1" to 10, "v_2" to 2.5)
            }

            withClue("Query-result") {
                result.toList() shouldBe listOf(4.0)
            }
        }
    }

    "Dividing a scalar with an Integer expression" {

        val result = karangoDriver.query {
            val a = LET("a", 10)
            RETURN(33 / a)
        }

        assertSoftly {

            withClue("AQL-Query") {
                result.query.query shouldBe """
                    |LET `l_a` = (@l_a_1)
                    |RETURN (@v_2 / `l_a`)
                    |
                """.trimMargin()
            }

            withClue("AQL-vars") {
                result.query.vars shouldBe mapOf("l_a_1" to 10, "v_2" to 33)
            }

            withClue("Query-result") {
                result.toList() shouldBe listOf(3.3)
            }
        }
    }

    "Division remainder of two Integer expressions" {

        val result = karangoDriver.query {
            val a = LET("a", 10)
            val b = LET("b", 20)
            RETURN(a % b)
        }

        assertSoftly {

            withClue("AQL-Query") {
                result.query.query shouldBe """
                    |LET `l_a` = (@l_a_1)
                    |LET `l_b` = (@l_b_2)
                    |RETURN (`l_a` % `l_b`)
                    |
                """.trimMargin()
            }

            withClue("AQL-vars") {
                result.query.vars shouldBe mapOf("l_a_1" to 10, "l_b_2" to 20)
            }

            withClue("Query-result") {
                result.toList() shouldBe listOf(10L)
            }
        }
    }

    "Division remainder of an Integer expression and a scalar" {

        val result = karangoDriver.query {
            val a = LET("a", 10)
            RETURN(a % 3)
        }

        assertSoftly {

            withClue("AQL-Query") {
                result.query.query shouldBe """
                    |LET `l_a` = (@l_a_1)
                    |RETURN (`l_a` % @v_2)
                    |
                """.trimMargin()
            }

            withClue("AQL-vars") {
                result.query.vars shouldBe mapOf("l_a_1" to 10, "v_2" to 3)
            }

            withClue("Query-result") {
                result.toList() shouldBe listOf(1L)
            }
        }
    }

    "Division remainder of a scalar and an Integer expression" {

        val result = karangoDriver.query {
            val a = LET("a", 10)
            RETURN(33 % a)
        }

        assertSoftly {

            withClue("AQL-Query") {
                result.query.query shouldBe """
                    |LET `l_a` = (@l_a_1)
                    |RETURN (@v_2 % `l_a`)
                    |
                """.trimMargin()
            }

            withClue("AQL-vars") {
                result.query.vars shouldBe mapOf("l_a_1" to 10, "v_2" to 33)
            }

            withClue("Query-result") {
                result.toList() shouldBe listOf(3L)
            }
        }
    }
})
