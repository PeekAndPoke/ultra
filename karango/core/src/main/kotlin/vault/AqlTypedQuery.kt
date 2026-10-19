package io.peekandpoke.karango.vault

import io.peekandpoke.karango.aql.AqlPrinter
import io.peekandpoke.karango.aql.AqlRootExpression
import io.peekandpoke.karango.aql.AqlTerminalExpr
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.vault.TypedQuery

data class AqlTypedQuery<T>(
    override val root: AqlTerminalExpr<T>,
    override val query: String,
    override val vars: Map<String, Any?>,
) : TypedQuery<T> {

    /**
     * The label the profiler records [query] under: [LANGUAGE] only for the text its DSL root printed,
     * [LANGUAGE_RAW] for anything else -- a query from [of], or a `copy` with other text.
     */
    internal val queryLanguage: String
        get() = if ((root as? AqlRootExpression<*>)?.printed?.query == query) LANGUAGE else LANGUAGE_RAW

    companion object {
        /**
         * Text printed from the query DSL, every value an `@placeholder`. `VaultCollector` keeps a query's
         * text by this label alone, so nothing else may be recorded under it.
         */
        const val LANGUAGE: String = "aql"

        /** Any other AQL text: it may hold literal values, so insights withholds it. */
        const val LANGUAGE_RAW: String = "aql-raw"

        /** A query from raw [query] text. Recorded as [LANGUAGE_RAW]: nothing guarantees its values are bound. */
        fun <T> of(type: TypeRef<T>, query: String = "", vars: Map<String, Any?> = emptyMap()) = AqlTypedQuery(
            root = EmptyTerminalExpr(type),
            query = query,
            vars = vars,
        )
    }

    private class EmptyTerminalExpr<T>(private val type: TypeRef<T>) : AqlTerminalExpr<T> {
        override fun getType(): TypeRef<List<T>> {
            return type.list
        }

        override fun innerType(): TypeRef<T> {
            return type
        }

        override fun print(p: AqlPrinter) {
            // noop
        }
    }
}
