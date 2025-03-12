@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.nthParamName
import de.peekandpoke.ultra.vault.lang.Expression
import de.peekandpoke.ultra.vault.lang.IterableExpr
import de.peekandpoke.ultra.vault.lang.Printer
import de.peekandpoke.ultra.vault.lang.Statement
import de.peekandpoke.ultra.vault.lang.TerminalExpr
import de.peekandpoke.ultra.vault.lang.VaultDslMarker
import kotlin.math.max

@VaultDslMarker
fun FOR(iteratorName: String) = ForLoop.For(iteratorName)

@VaultDslMarker
fun <T, R> FOR(
    name: String,
    iterable: Expression<List<T>>,
    builder: ForLoop.(IterableExpr<T>) -> TerminalExpr<R>,
): TerminalExpr<R> {
    return FOR(name) IN (iterable.invoke(builder))
}

@VaultDslMarker
fun <T, R> FOR(iterable: Expression<List<T>>, builder: ForLoop.(IterableExpr<T>) -> TerminalExpr<R>): TerminalExpr<R> =
    FOR(builder.nthParamName(1), iterable, builder)

@VaultDslMarker
operator fun <T, R> Expression<List<T>>.invoke(builder: ForLoop.(IterableExpr<T>) -> TerminalExpr<R>) =
    ForLoop.In(this, builder)

@VaultDslMarker
class ForLoop internal constructor() : StatementBuilder {

    @VaultDslMarker
    class For(private val iteratorName: String) {

        @VaultDslMarker
        infix fun <T, R> IN(forIn: In<T, R>): TerminalExpr<R> {

            val loop = ForLoop()
            val iterator = IterableExpr(iteratorName, forIn.iterable)
            val returns = forIn.builder(loop, iterator)

            return ForLoopExpr(iterator, forIn.iterable, loop.stmts, returns)
        }
    }

    @VaultDslMarker
    class In<T, R>(
        internal val iterable: Expression<List<T>>,
        internal val builder: ForLoop.(IterableExpr<T>) -> TerminalExpr<R>,
    )

    override val stmts = mutableListOf<Statement>()

    @VaultDslMarker
    fun FILTER(predicate: Expression<Boolean>): Unit = run {
        Filter(predicate).addStmt()
    }

    @VaultDslMarker
    fun FILTER_ANY(vararg predicate: Expression<Boolean>): Unit =
        FILTER(predicate.toList().any)

    @VaultDslMarker
    fun SORT(vararg sorts: Sort): Unit = run { SortBy(sorts.toList()).addStmt() }

    @VaultDslMarker
    fun <T> SORT(expr: Expression<T>, direction: Direction = Direction.ASC): Unit = SORT(expr.sort(direction))

    @VaultDslMarker
    fun LIMIT(limit: Int): Unit = run { OffsetAndLimit(0, limit).addStmt() }

    @VaultDslMarker
    fun LIMIT(offset: Int, limit: Int): Unit = run { OffsetAndLimit(offset, limit).addStmt() }

    @VaultDslMarker
    fun SKIP(skip: Int): Unit = run { OffsetAndLimit(skip, null).addStmt() }

    @VaultDslMarker
    fun PAGE(page: Int = 1, epp: Int = 20) = LIMIT(offset = max(0, page - 1) * epp, limit = epp)
}


internal class ForLoopExpr<T, R>(
    private val iterator: IterableExpr<T>,
    private val iterable: Expression<List<T>>,
    private val stmts: List<Statement>,
    private val ret: TerminalExpr<R>,
) : TerminalExpr<R> {

    override fun getType() = ret.getType()

    override fun innerType() = ret.innerType()

    override fun print(p: Printer) = with(p) {

        append("FOR ").append(iterator).append(" IN ").append(iterable).appendLine()

        indent {
            append(stmts)
            append(ret)
        }
    }
}
