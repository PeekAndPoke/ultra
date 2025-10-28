@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.TypeRef

data class AqlFilterStatement(private val predicate: AqlExpression<Boolean>) : AqlStatement {
    override fun print(p: AqlPrinter) {
        p.append("FILTER ").append(predicate).appendLine()
    }
}

data class AqlFilterByExpression<L, R>(
    val left: AqlExpression<L>,
    val op: AqlBooleanOperator,
    val right: AqlExpression<R>,
) : AqlExpression<Boolean> {

    companion object {

        fun <X> value(left: AqlExpression<X>, op: AqlBooleanOperator, right: X?) =
            AqlFilterByExpression(left, op, AqlValueExpr(type = left.getType().nullable, right))

        fun <XL, XR> expr(left: AqlExpression<XL>, op: AqlBooleanOperator, right: AqlExpression<XR>) =
            AqlFilterByExpression(left, op, right)
    }

    override fun getType() = TypeRef.Boolean

    override fun print(p: AqlPrinter) {
        p.append(left).append(" ${op.op} ").append(right)
    }
}

data class AqlFilterLogicExpression(
    val left: AqlExpression<Boolean>,
    val op: AqlLogicOperator,
    val right: AqlExpression<Boolean>,
) : AqlExpression<Boolean> {

    override fun getType() = TypeRef.Boolean

    override fun print(p: AqlPrinter) {
        p.append("(").append(left).append(")")
            .append(" ${op.op} ")
            .append("(").append(right).append(")")
    }
}
