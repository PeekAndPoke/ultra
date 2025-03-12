package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.vault.lang.ValueExpr
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.fail
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

class OperatorArithmeticSpec : StringSpec({

    val samples = listOf(
        // plus operator
        row("plus: Expression<String> + Expression", "a".aql + 20.aql, ArithmeticOperator.PLUS),
        row("plus: Expression + Expression", 10.aql + 20.aql, ArithmeticOperator.PLUS),
        row("plus: Expression + Expression<String>", 10.aql + "a".aql, ArithmeticOperator.PLUS),
        row("plus: Expression + Int", 10.aql + 20, ArithmeticOperator.PLUS),
        row("plus: Expression + Double", 10.aql + 20.0, ArithmeticOperator.PLUS),
        row("plus: Int + Expression", 10 + 20.aql, ArithmeticOperator.PLUS),
        row("plus: Double + Expression", 10.0 + 20.aql, ArithmeticOperator.PLUS),

        // minus operator
        row("minus: Expression<String> - Expression", "a".aql - 20.aql, ArithmeticOperator.MINUS),
        row("minus: Expression - Expression", 10.aql - 20.aql, ArithmeticOperator.MINUS),
        row("minus: Expression - Expression<String>", 10.aql - "a".aql, ArithmeticOperator.MINUS),
        row("minus: Expression - Int", 10.aql - 20, ArithmeticOperator.MINUS),
        row("minus: Expression - Double", 10.aql - 20.0, ArithmeticOperator.MINUS),
        row("minus: Int - Expression", 10 - 20.aql, ArithmeticOperator.MINUS),
        row("minus: Double - Expression", 10.0 - 20.aql, ArithmeticOperator.MINUS),

        // times operator
        row("times: Expression<String> * Expression", "a".aql * 20.aql, ArithmeticOperator.TIMES),
        row("times: Expression * Expression", 10.aql * 20.aql, ArithmeticOperator.TIMES),
        row("times: Expression * Expression<String>", 10.aql * "a".aql, ArithmeticOperator.TIMES),
        row("times: Expression * Int", 10.aql * 20, ArithmeticOperator.TIMES),
        row("times: Expression * Double", 10.aql * 20.0, ArithmeticOperator.TIMES),
        row("times: Int * Expression", 10 * 20.aql, ArithmeticOperator.TIMES),
        row("times: Double * Expression", 10.0 * 20.aql, ArithmeticOperator.TIMES),

        // times operator
        row("div: Expression<String> / Expression", "a".aql / 20.aql, ArithmeticOperator.DIV),
        row("div: Expression / Expression", 10.aql / 20.aql, ArithmeticOperator.DIV),
        row("div: Expression / Expression<String>", 10.aql / "a".aql, ArithmeticOperator.DIV),
        row("div: Expression / Int", 10.aql / 20, ArithmeticOperator.DIV),
        row("div: Expression / Double", 10.aql / 20.0, ArithmeticOperator.DIV),
        row("div: Int / Expression", 10 / 20.aql, ArithmeticOperator.DIV),
        row("div: Double / Expression", 10.0 / 20.aql, ArithmeticOperator.DIV),

        // times operator
        row("rem: Expression<String> % Expression", "a".aql % 20.aql, ArithmeticOperator.REM),
        row("rem: Expression % Expression", 10.aql % 20.aql, ArithmeticOperator.REM),
        row("rem: Expression % Expression<String>", 10.aql % "a".aql, ArithmeticOperator.REM),
        row("rem: Expression % Int", 10.aql % 20, ArithmeticOperator.REM),
        row("rem: Expression % Double", 10.aql % 20.0, ArithmeticOperator.REM),
        row("rem: Int % Expression", 10 % 20.aql, ArithmeticOperator.REM),
        row("rem: Double % Expression", 10.0 % 20.aql, ArithmeticOperator.REM)
    )

    for ((description, expression, operator) in samples) {

        "operator $description" {

            if (expression is ArithmeticExpression<*, *>) {

                assertSoftly {
                    expression.left::class shouldBe ValueExpr::class
                    expression.op shouldBe operator
                    expression.right::class shouldBe ValueExpr::class
                }

            } else {
                fail("invalid result type")
            }
        }
    }

    "Printing arithmetic expressions must work" {

        val source = "a".aql + 10.aql
        val result = source.print()

        assertSoftly {
            result.query shouldBe "(@v_1 + @v_2)"
            result.raw shouldBe "(\"a\" + 10)"
        }
    }
})
