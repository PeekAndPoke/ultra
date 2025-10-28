@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.nthParamName
import de.peekandpoke.ultra.vault.lang.VaultDslMarker
import kotlin.math.max

@VaultDslMarker
fun FOR(name: String) = AqlForLoop.For(iteratorName = name)

@VaultDslMarker
fun <T, R> FOR(
    name: String,
    iterable: AqlExpression<List<T>>,
    builder: AqlForLoop.(AqlIterableExpr<T>) -> AqlTerminalExpr<R>,
): AqlTerminalExpr<R> {
    return FOR(name) IN (iterable.invoke(builder))
}

@VaultDslMarker
fun <T, R> FOR(
    iterable: AqlExpression<List<T>>,
    builder: AqlForLoop.(AqlIterableExpr<T>) -> AqlTerminalExpr<R>,
): AqlTerminalExpr<R> =
    FOR(name = builder.nthParamName(1), iterable = iterable, builder = builder)

@VaultDslMarker
operator fun <T, R> AqlExpression<List<T>>.invoke(builder: AqlForLoop.(AqlIterableExpr<T>) -> AqlTerminalExpr<R>) =
    AqlForLoop.In(iterable = this, builder = builder)

@VaultDslMarker
class AqlForLoop internal constructor() : AqlStatementBuilder {

    @VaultDslMarker
    class For(private val iteratorName: String) {

        @VaultDslMarker
        infix fun <T, R> IN(forIn: In<T, R>): AqlTerminalExpr<R> {

            val loop = AqlForLoop()
            val iterator = AqlIterableExpr(iteratorName, forIn.iterable)
            val returns = forIn.builder(loop, iterator)

            return AqlForLoopExpr(iterator, forIn.iterable, loop.stmts, returns)
        }
    }

    @VaultDslMarker
    class In<T, R>(
        internal val iterable: AqlExpression<List<T>>,
        internal val builder: AqlForLoop.(AqlIterableExpr<T>) -> AqlTerminalExpr<R>,
    )

    override val stmts = mutableListOf<AqlStatement>()

    @VaultDslMarker
    fun FILTER(predicate: AqlExpression<Boolean>): Unit = run {
        AqlFilterStatement(predicate).addStmt()
    }

    @VaultDslMarker
    fun FILTER_ANY(vararg predicate: AqlExpression<Boolean>) {
        FILTER(predicate.toList().any)
    }

    @VaultDslMarker
    fun SORT(vararg sorts: AqlSorting) {
        AqlSortByStmt(sorts.toList()).addStmt()
    }

    @VaultDslMarker
    fun <T> SORT(expr: AqlExpression<T>, direction: AqlSortDirection = AqlSortDirection.ASC) {
        SORT(expr.sort(direction))
    }

    @VaultDslMarker
    fun LIMIT(limit: Int) {
        AqlOffsetAndLimitStmt(0, limit).addStmt()
    }

    @VaultDslMarker
    fun LIMIT(offset: Int, limit: Int) {
        AqlOffsetAndLimitStmt(offset, limit).addStmt()
    }

    @VaultDslMarker
    fun SKIP(skip: Int) {
        AqlOffsetAndLimitStmt(skip, null).addStmt()
    }

    @VaultDslMarker
    fun PAGE(page: Int = 1, epp: Int = 20) {
        LIMIT(offset = max(0, page - 1) * epp, limit = epp)
    }
}

internal class AqlForLoopExpr<T, R>(
    private val iterator: AqlIterableExpr<T>,
    private val iterable: AqlExpression<List<T>>,
    private val stmts: List<AqlStatement>,
    private val ret: AqlTerminalExpr<R>,
) : AqlTerminalExpr<R> {

    override fun getType() = ret.getType()

    override fun innerType() = ret.innerType()

    override fun print(p: AqlPrinter) {

        p.append("FOR ").append(iterator).append(" IN ").append(iterable).appendLine()

        p.indent {
            append(stmts)
            append(ret)
        }
    }
}
