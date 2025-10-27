@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.vault.lang.VaultTerminalExpressionMarker

@Suppress("UnusedReceiverParameter")
@VaultTerminalExpressionMarker
fun <R> AqlStatementBuilder.RETURN(ret: AqlExpression<R>): AqlTerminalExpr<R> =
    AqlReturn(ret)

@Suppress("UnusedReceiverParameter")
@VaultTerminalExpressionMarker
fun <R> AqlStatementBuilder.RETURN_OLD(ret: AqlExpression<R>): AqlTerminalExpr<R> =
    AqlReturnOld(ret)

@Suppress("UnusedReceiverParameter")
@VaultTerminalExpressionMarker
fun <R> AqlStatementBuilder.RETURN_NEW(ret: AqlExpression<R>): AqlTerminalExpr<R> =
    AqlReturnNew(ret)

@Suppress("UnusedReceiverParameter")
@VaultTerminalExpressionMarker
fun <R> AqlStatementBuilder.RETURN_DISTINCT(ret: AqlExpression<R>): AqlTerminalExpr<R> =
    AqlReturnDistinct(ret)

@VaultTerminalExpressionMarker
fun AqlStatementBuilder.RETURN_COUNT(variableName: String = "count"): AqlTerminalExpr<Int> {

    val count: AqlExpression<Int> = COLLECT_WITH(Aql.COUNT, variableName)

    return RETURN(count)
}

internal class AqlReturn<T>(private val expression: AqlExpression<T>) : AqlTerminalExpr<T> {
    override fun innerType() = expression.getType()
    override fun getType(): TypeRef<List<T>> = expression.getType().list
    override fun print(p: AqlPrinter) {
        p.append("RETURN ").append(expression).nl()
    }
}

internal class AqlReturnOld<T>(private val expression: AqlExpression<T>) : AqlTerminalExpr<T> {
    override fun innerType() = expression.getType()
    override fun getType(): TypeRef<List<T>> = expression.getType().list
    override fun print(p: AqlPrinter) {
        p.append("RETURN OLD").nl()
    }
}

internal class AqlReturnNew<T>(private val expression: AqlExpression<T>) : AqlTerminalExpr<T> {
    override fun innerType() = expression.getType()
    override fun getType(): TypeRef<List<T>> = expression.getType().list
    override fun print(p: AqlPrinter) {
        p.append("RETURN NEW").nl()
    }
}

internal class AqlReturnDistinct<T>(private val expression: AqlExpression<T>) : AqlTerminalExpr<T> {
    override fun innerType() = expression.getType()
    override fun getType(): TypeRef<List<T>> = expression.getType().list
    override fun print(p: AqlPrinter) {
        p.append("RETURN DISTINCT ").append(expression).nl()
    }
}
