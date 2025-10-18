package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.model.tuple
import de.peekandpoke.ultra.vault.lang.ValueExpr
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.fail

class OperatorArithmeticSpec : StringSpec({

    val samples = listOf(
        // plus operator
        tuple("plus: Expression<String> + Expression", "a".aql + 20.aql, ArithmeticOperator.PLUS),
        tuple("plus: Expression + Expression", 10.aql + 20.aql, ArithmeticOperator.PLUS),
        tuple("plus: Expression + Expression<String>", 10.aql + "a".aql, ArithmeticOperator.PLUS),
        tuple("plus: Expression + Int", 10.aql + 20, ArithmeticOperator.PLUS),
        tuple("plus: Expression + Double", 10.aql + 20.0, ArithmeticOperator.PLUS),
        tuple("plus: Int + Expression", 10 + 20.aql, ArithmeticOperator.PLUS),
        tuple("plus: Double + Expression", 10.0 + 20.aql, ArithmeticOperator.PLUS),

        // minus operator
        tuple("minus: Expression<String> - Expression", "a".aql - 20.aql, ArithmeticOperator.MINUS),
        tuple("minus: Expression - Expression", 10.aql - 20.aql, ArithmeticOperator.MINUS),
        tuple("minus: Expression - Expression<String>", 10.aql - "a".aql, ArithmeticOperator.MINUS),
        tuple("minus: Expression - Int", 10.aql - 20, ArithmeticOperator.MINUS),
        tuple("minus: Expression - Double", 10.aql - 20.0, ArithmeticOperator.MINUS),
        tuple("minus: Int - Expression", 10 - 20.aql, ArithmeticOperator.MINUS),
        tuple("minus: Double - Expression", 10.0 - 20.aql, ArithmeticOperator.MINUS),

        // times operator
        tuple("times: Expression<String> * Expression", "a".aql * 20.aql, ArithmeticOperator.TIMES),
        tuple("times: Expression * Expression", 10.aql * 20.aql, ArithmeticOperator.TIMES),
        tuple("times: Expression * Expression<String>", 10.aql * "a".aql, ArithmeticOperator.TIMES),
        tuple("times: Expression * Int", 10.aql * 20, ArithmeticOperator.TIMES),
        tuple("times: Expression * Double", 10.aql * 20.0, ArithmeticOperator.TIMES),
        tuple("times: Int * Expression", 10 * 20.aql, ArithmeticOperator.TIMES),
        tuple("times: Double * Expression", 10.0 * 20.aql, ArithmeticOperator.TIMES),

        // times operator
        tuple("div: Expression<String> / Expression", "a".aql / 20.aql, ArithmeticOperator.DIV),
        tuple("div: Expression / Expression", 10.aql / 20.aql, ArithmeticOperator.DIV),
        tuple("div: Expression / Expression<String>", 10.aql / "a".aql, ArithmeticOperator.DIV),
        tuple("div: Expression / Int", 10.aql / 20, ArithmeticOperator.DIV),
        tuple("div: Expression / Double", 10.aql / 20.0, ArithmeticOperator.DIV),
        tuple("div: Int / Expression", 10 / 20.aql, ArithmeticOperator.DIV),
        tuple("div: Double / Expression", 10.0 / 20.aql, ArithmeticOperator.DIV),

        // times operator
        tuple("rem: Expression<String> % Expression", "a".aql % 20.aql, ArithmeticOperator.REM),
        tuple("rem: Expression % Expression", 10.aql % 20.aql, ArithmeticOperator.REM),
        tuple("rem: Expression % Expression<String>", 10.aql % "a".aql, ArithmeticOperator.REM),
        tuple("rem: Expression % Int", 10.aql % 20, ArithmeticOperator.REM),
        tuple("rem: Expression % Double", 10.aql % 20.0, ArithmeticOperator.REM),
        tuple("rem: Int % Expression", 10 % 20.aql, ArithmeticOperator.REM),
        tuple("rem: Double % Expression", 10.0 % 20.aql, ArithmeticOperator.REM)
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
