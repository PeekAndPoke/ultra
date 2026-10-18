@file:Suppress("FunctionName")

package io.peekandpoke.karango.aql

import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType

fun <T> AqlStatementBuilder.LET(
    name: String,
    expression: AqlExpression<T>,
): AqlExpression<T> = AqlLetExpr(name, expression).addStmt().toExpression()

fun AqlStatementBuilder.LET(
    name: String,
    @Suppress("UNUSED_PARAMETER") value: Nothing?,
): AqlExpression<Any?> = LET(name = name, expression = AqlValueExpr.Null())

inline fun <reified T> AqlStatementBuilder.LET(
    name: String,
    value: T,
): AqlExpression<T> = AqlLetStmt(name, value, kType()).addStmt().toExpression()

inline fun <reified T> AqlStatementBuilder.LET(
    name: String,
    builder: () -> T,
): AqlExpression<T> = AqlLetStmt(name, builder(), kType()).addStmt().toExpression()

/**
 * Let statement created from a user value
 */
class AqlLetStmt<T>(name: String, private val value: T, type: TypeRef<T>) : AqlStatement {

    private val lName = "l_$name"

    private val expression: AqlExpression<T> = AqlNameExpr(lName, type)

    fun toExpression(): AqlExpression<T> = expression

    override fun print(p: AqlPrinter) {
        p.append("LET ").name(lName).append(" = (").value(lName, value as Any).append(")")
        p.nl()
    }
}

/**
 * Let statement created from an expression
 */
class AqlLetExpr<T>(name: String, private val value: AqlExpression<T>) : AqlStatement {

    private val lName = "l_$name"

    private val expression: AqlExpression<T> = AqlNameExpr(lName, value.getType())

    fun toExpression(): AqlExpression<T> = expression

    override fun print(p: AqlPrinter) {
        p.append("LET ").name(lName).append(" = (").append(value).append(")")
        p.appendLine()
    }
}
