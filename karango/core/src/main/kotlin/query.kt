package io.peekandpoke.karango

import com.arangodb.model.AqlQueryOptions
import io.peekandpoke.karango.aql.AqlPrinter
import io.peekandpoke.karango.aql.AqlRootExpression
import io.peekandpoke.karango.aql.AqlStatementBuilderImpl
import io.peekandpoke.karango.aql.AqlTerminalExpr
import io.peekandpoke.karango.vault.AqlTypedQuery

typealias AqlQueryOptionProvider = (AqlQueryOptions) -> AqlQueryOptions

/** Builds a type-safe AQL query from the DSL [builder] and returns the compiled [AqlTypedQuery]. */
fun <T> buildAqlQuery(builder: AqlStatementBuilderImpl.() -> AqlTerminalExpr<T>): AqlTypedQuery<T> {

    val root = AqlRootExpression.from(builder)

    val query = AqlPrinter().append(root).build()

    return AqlTypedQuery(root = root, query = query.query, vars = query.vars)
}
