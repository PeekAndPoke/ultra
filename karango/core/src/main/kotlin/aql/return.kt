@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.vault.lang.Expression
import de.peekandpoke.ultra.vault.lang.Printer
import de.peekandpoke.ultra.vault.lang.TerminalExpr
import de.peekandpoke.ultra.vault.lang.VaultTerminalExpressionMarker

@Suppress("UnusedReceiverParameter")
@VaultTerminalExpressionMarker
fun <R> StatementBuilder.RETURN(ret: Expression<R>): TerminalExpr<R> = Return(ret)

@Suppress("UnusedReceiverParameter")
@VaultTerminalExpressionMarker
fun <R> StatementBuilder.RETURN_OLD(ret: Expression<R>): TerminalExpr<R> = ReturnOld(ret)

@Suppress("UnusedReceiverParameter")
@VaultTerminalExpressionMarker
fun <R> StatementBuilder.RETURN_NEW(ret: Expression<R>): TerminalExpr<R> = ReturnNew(ret)

@Suppress("UnusedReceiverParameter")
@VaultTerminalExpressionMarker
fun <R> StatementBuilder.RETURN_DISTINCT(ret: Expression<R>): TerminalExpr<R> = ReturnDistinct(ret)

@VaultTerminalExpressionMarker
fun StatementBuilder.RETURN_COUNT(variableName: String = "count"): TerminalExpr<Int> {

    val count: Expression<Int> = COLLECT_WITH(Aql.COUNT, variableName)

    return RETURN(
        count
    )
}

internal class Return<T>(private val expression: Expression<T>) : TerminalExpr<T> {
    override fun innerType() = expression.getType()
    override fun getType(): TypeRef<List<T>> = expression.getType().list
    override fun print(p: Printer) = p.append("RETURN ").append(expression).nl()
}

internal class ReturnOld<T>(private val expression: Expression<T>) : TerminalExpr<T> {
    override fun innerType() = expression.getType()
    override fun getType(): TypeRef<List<T>> = expression.getType().list
    override fun print(p: Printer) = p.append("RETURN OLD").nl()
}

internal class ReturnNew<T>(private val expression: Expression<T>) : TerminalExpr<T> {
    override fun innerType() = expression.getType()
    override fun getType(): TypeRef<List<T>> = expression.getType().list
    override fun print(p: Printer) = p.append("RETURN NEW").nl()
}

internal class ReturnDistinct<T>(private val expression: Expression<T>) : TerminalExpr<T> {
    override fun innerType() = expression.getType()
    override fun getType(): TypeRef<List<T>> = expression.getType().list
    override fun print(p: Printer) = p.append("RETURN DISTINCT ").append(expression).nl()
}
