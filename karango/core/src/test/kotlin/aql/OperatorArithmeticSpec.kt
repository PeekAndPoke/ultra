package de.peekandpoke.karango.aql

import de.peekandpoke.karango.aql.AqlPrinter.Companion.print
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.fail

class OperatorArithmeticSpec : StringSpec({

    val samples = listOf(
        // plus operator
        tuple("plus: Expression<String> + Expression", "a".aql + 20.aql, AqlArithmeticOperator.PLUS),
        tuple("plus: Expression + Expression", 10.aql + 20.aql, AqlArithmeticOperator.PLUS),
        tuple("plus: Expression + Expression<String>", 10.aql + "a".aql, AqlArithmeticOperator.PLUS),
        tuple("plus: Expression + Int", 10.aql + 20, AqlArithmeticOperator.PLUS),
        tuple("plus: Expression + Double", 10.aql + 20.0, AqlArithmeticOperator.PLUS),
        tuple("plus: Int + Expression", 10 + 20.aql, AqlArithmeticOperator.PLUS),
        tuple("plus: Double + Expression", 10.0 + 20.aql, AqlArithmeticOperator.PLUS),

        // minus operator
        tuple("minus: Expression<String> - Expression", "a".aql - 20.aql, AqlArithmeticOperator.MINUS),
        tuple("minus: Expression - Expression", 10.aql - 20.aql, AqlArithmeticOperator.MINUS),
        tuple("minus: Expression - Expression<String>", 10.aql - "a".aql, AqlArithmeticOperator.MINUS),
        tuple("minus: Expression - Int", 10.aql - 20, AqlArithmeticOperator.MINUS),
        tuple("minus: Expression - Double", 10.aql - 20.0, AqlArithmeticOperator.MINUS),
        tuple("minus: Int - Expression", 10 - 20.aql, AqlArithmeticOperator.MINUS),
        tuple("minus: Double - Expression", 10.0 - 20.aql, AqlArithmeticOperator.MINUS),

        // times operator
        tuple("times: Expression<String> * Expression", "a".aql * 20.aql, AqlArithmeticOperator.TIMES),
        tuple("times: Expression * Expression", 10.aql * 20.aql, AqlArithmeticOperator.TIMES),
        tuple("times: Expression * Expression<String>", 10.aql * "a".aql, AqlArithmeticOperator.TIMES),
        tuple("times: Expression * Int", 10.aql * 20, AqlArithmeticOperator.TIMES),
        tuple("times: Expression * Double", 10.aql * 20.0, AqlArithmeticOperator.TIMES),
        tuple("times: Int * Expression", 10 * 20.aql, AqlArithmeticOperator.TIMES),
        tuple("times: Double * Expression", 10.0 * 20.aql, AqlArithmeticOperator.TIMES),

        // times operator
        tuple("div: Expression<String> / Expression", "a".aql / 20.aql, AqlArithmeticOperator.DIV),
        tuple("div: Expression / Expression", 10.aql / 20.aql, AqlArithmeticOperator.DIV),
        tuple("div: Expression / Expression<String>", 10.aql / "a".aql, AqlArithmeticOperator.DIV),
        tuple("div: Expression / Int", 10.aql / 20, AqlArithmeticOperator.DIV),
        tuple("div: Expression / Double", 10.aql / 20.0, AqlArithmeticOperator.DIV),
        tuple("div: Int / Expression", 10 / 20.aql, AqlArithmeticOperator.DIV),
        tuple("div: Double / Expression", 10.0 / 20.aql, AqlArithmeticOperator.DIV),

        // times operator
        tuple("rem: Expression<String> % Expression", "a".aql % 20.aql, AqlArithmeticOperator.REM),
        tuple("rem: Expression % Expression", 10.aql % 20.aql, AqlArithmeticOperator.REM),
        tuple("rem: Expression % Expression<String>", 10.aql % "a".aql, AqlArithmeticOperator.REM),
        tuple("rem: Expression % Int", 10.aql % 20, AqlArithmeticOperator.REM),
        tuple("rem: Expression % Double", 10.aql % 20.0, AqlArithmeticOperator.REM),
        tuple("rem: Int % Expression", 10 % 20.aql, AqlArithmeticOperator.REM),
        tuple("rem: Double % Expression", 10.0 % 20.aql, AqlArithmeticOperator.REM)
    )

    for ((description, expression, operator) in samples) {

        "operator $description" {

            if (expression is AqlArithmeticExpression<*, *>) {

                assertSoftly {
                    expression.left::class shouldBe AqlValueExpr::class
                    expression.op shouldBe operator
                    expression.right::class shouldBe AqlValueExpr::class
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
