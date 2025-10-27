package de.peekandpoke.karango

import com.arangodb.model.AqlQueryOptions
import de.peekandpoke.karango.aql.AqlPrinter
import de.peekandpoke.karango.aql.AqlRootExpression
import de.peekandpoke.karango.aql.AqlStatementBuilderImpl
import de.peekandpoke.karango.aql.AqlTerminalExpr
import de.peekandpoke.karango.vault.AqlTypedQuery

typealias AqlQueryOptionProvider = (AqlQueryOptions) -> AqlQueryOptions

fun <T> buildAqlQuery(builder: AqlStatementBuilderImpl.() -> AqlTerminalExpr<T>): AqlTypedQuery<T> {

    val root = AqlRootExpression.from(builder)

    val query = AqlPrinter().append(root).build()

    return AqlTypedQuery(root = root, query = query.query, vars = query.vars)
}
