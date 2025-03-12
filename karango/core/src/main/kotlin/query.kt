package de.peekandpoke.karango

import com.arangodb.model.AqlQueryOptions
import de.peekandpoke.karango.aql.AqlBuilder
import de.peekandpoke.karango.aql.AqlPrinter
import de.peekandpoke.karango.aql.RootExpression
import de.peekandpoke.ultra.vault.TypedQuery
import de.peekandpoke.ultra.vault.lang.TerminalExpr

typealias AqlQueryOptionProvider = (AqlQueryOptions) -> AqlQueryOptions

fun <T> buildQuery(builder: AqlBuilder.() -> TerminalExpr<T>): TypedQuery<T> {

    val root = RootExpression.from(builder)

    val query = AqlPrinter().append(root).build()

    return TypedQuery(root, query.query, query.vars)
}
