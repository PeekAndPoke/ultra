package de.peekandpoke.ultra.vault

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.vault.lang.Printer
import de.peekandpoke.ultra.vault.lang.TerminalExpr

data class TypedQuery<T>(val root: TerminalExpr<T>, val query: String, val vars: Map<String, Any?>) {

    companion object {
        fun <T> empty(type: TypeRef<T>) = TypedQuery(
            root = EmptyTerminalExpr(type),
            query = "",
            vars = emptyMap(),
        )
    }

    private class EmptyTerminalExpr<T>(private val type: TypeRef<T>) : TerminalExpr<T> {
        override fun print(p: Printer): Any {
            // noop
            return Unit
        }

        override fun getType(): TypeRef<List<T>> {
            return type.list
        }

        override fun innerType(): TypeRef<T> {
            return type
        }
    }
}
