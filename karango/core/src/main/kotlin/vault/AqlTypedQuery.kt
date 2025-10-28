package de.peekandpoke.karango.vault

import de.peekandpoke.karango.aql.AqlPrinter
import de.peekandpoke.karango.aql.AqlTerminalExpr
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.vault.TypedQuery

data class AqlTypedQuery<T>(
    override val root: AqlTerminalExpr<T>,
    override val query: String,
    override val vars: Map<String, Any?>,
) : TypedQuery<T> {

    companion object {
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
