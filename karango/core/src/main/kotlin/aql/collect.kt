@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

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
 *     val count: AqlExpression<Int> = COLLECT_WITH(Aql.COUNT, "variable")
 * ```
 */
@Suppress("unused")
@VaultTerminalExpressionMarker
fun <T> AqlStatementBuilder.COLLECT_WITH(with: AqlFunctionDefinition<T>, into: String): AqlExpression<T> {

    AqlCollectWithExpressionStmt(with, into).addStmt()

    return AqlNameExpr(into, with.getType())
}

@Suppress("unused")
@VaultDslMarker
fun <T> AqlStatementBuilder.COLLECT(
    assignee: String,
    expr: AqlExpression<T>,
): AqlCollectStmt<T> {
    return AqlCollectStmt(assignee = assignee, expr = expr).addStmt()
}

@Suppress("unused")
@VaultDslMarker
fun <T> AqlStatementBuilder.COLLECT_INTO(
    assignee: String,
    expr: AqlExpression<T>,
    into: String,
): AqlCollectIntoStmt<T> {
    return AqlCollectIntoStmt(assignee = assignee, expr = expr, into = into).addStmt()
}

@Suppress("unused")
@VaultDslMarker
fun <T, A> AqlStatementBuilder.COLLECT_AGGREGATE(
    group: String,
    groupBy: AqlExpression<T>,
    aggregate: String,
    aggregateWith: AqlExpression<A>,
): AqlCollectAggregateStmt<T, A> {

    return AqlCollectAggregateStmt(
        group = group,
        groupBy = groupBy,
        aggregate = aggregate,
        aggregateWith = aggregateWith
    ).addStmt()
}

@VaultDslMarker
class AqlCollectWithExpressionStmt<T> internal constructor(
    private val func: AqlFunctionDefinition<T>,
    private val variable: String,
) : AqlStatement {
    override fun print(p: AqlPrinter) {
        with(p) {
            append("COLLECT WITH ").append(func).append(" INTO ").append(variable).nl()
        }
    }
}

@VaultDslMarker
class AqlCollectStmt<T> internal constructor(
    assignee: String,
    private val expr: AqlExpression<T>,
) : AqlStatement {

    val assignee: AqlNameExpr<T> = AqlNameExpr(assignee, expr.getType())

    override fun print(p: AqlPrinter) {
        p.append("COLLECT ")
        p.append(assignee).append(" = ").append(expr)
        p.nl()
    }
}

@VaultDslMarker
class AqlCollectIntoStmt<T> internal constructor(
    assignee: String,
    private val expr: AqlExpression<T>,
    into: String,
) : AqlStatement {

    val assignee: AqlNameExpr<T> = AqlNameExpr(assignee, expr.getType())
    val into: AqlNameExpr<List<T>> = AqlNameExpr(into, expr.getType().list)

    override fun print(p: AqlPrinter) {
        p.append("COLLECT ")
        p.append(assignee).append(" = ").append(expr)
        p.append(" INTO ").append(into)
        p.nl()
    }
}

@VaultDslMarker
class AqlCollectAggregateStmt<G, A> internal constructor(
    group: String,
    private val groupBy: AqlExpression<G>,
    aggregate: String,
    private val aggregateWith: AqlExpression<A>,
) : AqlStatement {

    val group: AqlNameExpr<G> = AqlNameExpr(group, groupBy.getType())
    val aggregate: AqlNameExpr<A> = AqlNameExpr(aggregate, aggregateWith.getType())

    override fun print(p: AqlPrinter) {
        p.append("COLLECT ")
        p.append(group).append(" = ").append(groupBy)
        p.append(" AGGREGATE ").append(aggregate).append(" = ").append(aggregateWith)
        p.nl()
    }
}
