package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.TypeRef

enum class AqlArithmeticOperator(val op: String) {
    PLUS("+"),
    MINUS("-"),
    TIMES("*"),
    DIV("/"),
    REM("%"),
}

internal class AqlArithmeticExpression<L, R>(
    internal val left: AqlExpression<L>,
    internal val op: AqlArithmeticOperator,
    internal val right: AqlExpression<R>,
) : AqlExpression<Number> {

    override fun getType() = TypeRef.Number

    override fun print(p: AqlPrinter) {
        p.append("(").append(left).append(" ${op.op} ").append(right).append(")")
    }
}

operator fun <L, R> AqlExpression<L>.plus(right: AqlExpression<R>): AqlExpression<Number> =
    AqlArithmeticExpression(this, AqlArithmeticOperator.PLUS, right)

operator fun <L> AqlExpression<L>.plus(right: Number): AqlExpression<Number> = this + right.aql()
operator fun <R> Number.plus(right: AqlExpression<R>): AqlExpression<Number> = this.aql() + right

operator fun <L, R> AqlExpression<L>.minus(right: AqlExpression<R>): AqlExpression<Number> =
    AqlArithmeticExpression(this, AqlArithmeticOperator.MINUS, right)

operator fun <L> AqlExpression<L>.minus(right: Number): AqlExpression<Number> = this - right.aql()
operator fun <R> Number.minus(right: AqlExpression<R>): AqlExpression<Number> = this.aql() - right

operator fun <L, R> AqlExpression<L>.times(right: AqlExpression<R>): AqlExpression<Number> =
    AqlArithmeticExpression(this, AqlArithmeticOperator.TIMES, right)

operator fun <L> AqlExpression<L>.times(right: Number): AqlExpression<Number> = this * right.aql()
operator fun <R> Number.times(right: AqlExpression<R>): AqlExpression<Number> = this.aql() * right

operator fun <L, R> AqlExpression<L>.div(right: AqlExpression<R>): AqlExpression<Number> =
    AqlArithmeticExpression(this, AqlArithmeticOperator.DIV, right)

operator fun <L> AqlExpression<L>.div(right: Number): AqlExpression<Number> = this / right.aql()
operator fun <R> Number.div(right: AqlExpression<R>): AqlExpression<Number> = this.aql() / right

operator fun <L, R> AqlExpression<L>.rem(right: AqlExpression<R>): AqlExpression<Number> =
    AqlArithmeticExpression(this, AqlArithmeticOperator.REM, right)

operator fun <L> AqlExpression<L>.rem(right: Number): AqlExpression<Number> = this % right.aql()
operator fun <R> Number.rem(right: AqlExpression<R>): AqlExpression<Number> = this.aql() % right
