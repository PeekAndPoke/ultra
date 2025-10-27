@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.lang.VaultDslMarker

@VaultDslMarker
fun <T> AqlStatementBuilder.LET(
    name: String,
    expression: AqlExpression<T>,
): AqlExpression<T> = AqlLetExpr(name, expression).addStmt().toExpression()

@VaultDslMarker
fun AqlStatementBuilder.LET(
    name: String,
    @Suppress("UNUSED_PARAMETER") value: Nothing?,
): AqlExpression<Any?> = LET(name = name, expression = AqlValueExpr.Null())

@VaultDslMarker
inline fun <reified T> AqlStatementBuilder.LET(
    name: String,
    value: T,
): AqlExpression<T> = AqlLetStmt(name, value, kType()).addStmt().toExpression()

@VaultDslMarker
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
