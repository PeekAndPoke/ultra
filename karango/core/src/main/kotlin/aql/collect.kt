@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.vault.lang.Expression
import de.peekandpoke.ultra.vault.lang.FunctionExpr
import de.peekandpoke.ultra.vault.lang.NameExpr
import de.peekandpoke.ultra.vault.lang.Printer
import de.peekandpoke.ultra.vault.lang.Statement
import de.peekandpoke.ultra.vault.lang.VaultDslMarker
import de.peekandpoke.ultra.vault.lang.VaultTerminalExpressionMarker

/**
 * Creates a COLLECT WITH expression like
 *
 * ```aql
 *     COLLECT WITH COUNT into `variable`
 * ```
 *
 * when used like this:
 *
 * ```kotlin
 *     val count: Expression<Int> = COLLECT_WITH(Aql.COUNT, "variable")
 * ```
 */
@Suppress("unused")
@VaultTerminalExpressionMarker
fun <T> StatementBuilder.COLLECT_WITH(with: FunctionExpr<T>, into: String): Expression<T> {

    CollectWithExpression(with, into).addStmt()

    return NameExpr(into, with.getType())
}

@Suppress("unused")
@VaultDslMarker
fun <T> StatementBuilder.COLLECT(assignee: String, expr: Expression<T>): CollectStatement<T> {
    return CollectStatement(assignee = assignee, expr = expr).addStmt()
}

@Suppress("unused")
@VaultDslMarker
fun <T> StatementBuilder.COLLECT_INTO(assignee: String, expr: Expression<T>, into: String): CollectIntoStatement<T> {
    return CollectIntoStatement(assignee = assignee, expr = expr, into = into).addStmt()
}

@Suppress("unused")
@VaultDslMarker
fun <T, A> StatementBuilder.COLLECT_AGGREGATE(
    group: String,
    groupBy: Expression<T>,
    aggregate: String,
    aggregateWith: Expression<A>,
): CollectAggregateStatement<T, A> {

    return CollectAggregateStatement(
        group = group,
        groupBy = groupBy,
        aggregate = aggregate,
        aggregateWith = aggregateWith
    ).addStmt()
}


@VaultDslMarker
class CollectWithExpression<T> internal constructor(
    private val func: FunctionExpr<T>,
    private val variable: String,
) : Statement {
    override fun print(p: Printer): Any = with(p) {
        append("COLLECT WITH ").append(func).append(" INTO ").append(variable).appendLine()
    }
}

@VaultDslMarker
class CollectStatement<T> internal constructor(
    assignee: String,
    private val expr: Expression<T>,
) : Statement {

    val assignee: NameExpr<T> = NameExpr(assignee, expr.getType())

    override fun print(p: Printer): Any = with(p) {
        append("COLLECT ")
            .append(assignee).append(" = ").append(this@CollectStatement.expr)
            .appendLine()
    }
}

@VaultDslMarker
class CollectIntoStatement<T> internal constructor(
    assignee: String,
    private val expr: Expression<T>,
    into: String,
) : Statement {

    val assignee: NameExpr<T> = NameExpr(assignee, expr.getType())
    val into: NameExpr<List<T>> = NameExpr(into, expr.getType().list)

    override fun print(p: Printer): Any = with(p) {
        append("COLLECT ")
            .append(assignee).append(" = ").append(this@CollectIntoStatement.expr)
            .append(" INTO ").append(into)
            .appendLine()
    }
}

@VaultDslMarker
class CollectAggregateStatement<G, A> internal constructor(
    group: String,
    private val groupBy: Expression<G>,
    aggregate: String,
    private val aggregateWith: Expression<A>,
) : Statement {

    val group: NameExpr<G> = NameExpr(group, groupBy.getType())
    val aggregate: NameExpr<A> = NameExpr(aggregate, aggregateWith.getType())

    override fun print(p: Printer): Any = with(p) {
        append("COLLECT ")
            .append(group).append(" = ").append(groupBy)
            .append(" AGGREGATE ").append(aggregate).append(" = ").append(aggregateWith)
            .appendLine()
    }
}
