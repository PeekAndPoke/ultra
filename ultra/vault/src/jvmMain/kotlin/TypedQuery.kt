package de.peekandpoke.ultra.vault

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.vault.lang.Expression

interface TypedQuery<T> {
    companion object {
        fun <T> of(returns: TypeRef<T>): TypedQuery<T> =
            TypedQueryImpl(
                root = EmptyExpression(returns.list),
                query = "",
                vars = emptyMap(),
            )
    }

    val root: Expression<List<T>>
    val query: String
    val vars: Map<String, Any?>
}

@PublishedApi
internal class EmptyExpression<T>(private val type: TypeRef<T>) : Expression<T> {
    override fun getType(): TypeRef<T> {
        return type
    }
}

@PublishedApi
internal class TypedQueryImpl<T>(
    override val root: Expression<List<T>>,
    override val query: String,
    override val vars: Map<String, Any?>,
) : TypedQuery<T>
