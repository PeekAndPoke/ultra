package io.peekandpoke.ultra.vault

import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.vault.lang.Expression

/**
 * A type-safe representation of a database query.
 *
 * Encapsulates the query string, its bind variables, and the root [Expression] that
 * carries the return type information needed for deserializing results.
 *
 * @param T the element type returned by this query.
 */
interface TypedQuery<T> {
    companion object {
        /**
         * Creates an empty [TypedQuery] that returns a list of [T] based on the given [TypeRef].
         */
        fun <T> of(returns: TypeRef<T>): TypedQuery<T> =
            TypedQueryImpl(
                root = EmptyExpression(returns.list),
                query = "",
                vars = emptyMap(),
            )
    }

    /** The root expression describing the return type of this query. */
    val root: Expression<List<T>>

    /** The raw query string (e.g. AQL). */
    val query: String

    /** Bind variables referenced by the [query]. */
    val vars: Map<String, Any?>
}

/**
 * An [Expression] that carries only type information and no query logic.
 * Used as the root expression for newly created [TypedQuery] instances.
 */
@PublishedApi
internal class EmptyExpression<T>(private val type: TypeRef<T>) : Expression<T> {
    override fun getType(): TypeRef<T> {
        return type
    }
}

/** Default implementation of [TypedQuery]. */
@PublishedApi
internal class TypedQueryImpl<T>(
    override val root: Expression<List<T>>,
    override val query: String,
    override val vars: Map<String, Any?>,
) : TypedQuery<T>
