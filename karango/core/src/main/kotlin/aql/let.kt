@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.lang.Expression
import de.peekandpoke.ultra.vault.lang.NameExpr
import de.peekandpoke.ultra.vault.lang.NullValueExpr
import de.peekandpoke.ultra.vault.lang.Printer
import de.peekandpoke.ultra.vault.lang.Statement
import de.peekandpoke.ultra.vault.lang.VaultDslMarker

@VaultDslMarker
fun <T> StatementBuilder.LET(
    name: String, expression: Expression<T>,
): Expression<T> = LetExpr(name, expression).addStmt().toExpression()

@VaultDslMarker
fun StatementBuilder.LET(
    name: String, @Suppress("UNUSED_PARAMETER") value: Nothing?,
): Expression<Any?> = LET(name, NullValueExpr())

@VaultDslMarker
inline fun <reified T> StatementBuilder.LET(
    name: String, value: T,
): Expression<T> = Let(name, value, kType()).addStmt().toExpression()

@VaultDslMarker
inline fun <reified T> StatementBuilder.LET(
    name: String, builder: () -> T,
): Expression<T> = Let(name, builder(), kType()).addStmt().toExpression()

/**
 * Let statement created from a user value
 */
class Let<T>(name: String, private val value: T, type: TypeRef<T>) : Statement {

    private val lName = "l_$name"

    private val expression: Expression<T> = NameExpr(lName, type)

    fun toExpression(): Expression<T> = expression

    override fun print(p: Printer) =
        p.append("LET ").name(lName).append(" = (").value(lName, value as Any).append(")").appendLine()
}

/**
 * Let statement created from an expression
 */
class LetExpr<T>(name: String, private val value: Expression<T>) : Statement {

    private val lName = "l_$name"

    private val expression: Expression<T> = NameExpr(lName, value.getType())

    fun toExpression(): Expression<T> = expression

    override fun print(p: Printer) =
        p.append("LET ").name(lName).append(" = (").append(value).append(")").appendLine()
}
