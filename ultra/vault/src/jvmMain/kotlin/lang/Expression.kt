package io.peekandpoke.ultra.vault.lang

import io.peekandpoke.ultra.reflection.TypeRef

/**
 * Base interface for all Expressions.
 *
 * @param T the value type the expression evaluates to.
 */
interface Expression<T> {
    /**
     * Returns a reference to the type that the expression represents.
     *
     * The type information is needed for un-serializing a query result.
     */
    fun getType(): TypeRef<T>

    /**
     * Re-types the expression from [T] to the child type [U].
     *
     * Unchecked: returns the same instance, so [getType] keeps reporting the original type and
     * query results are still un-serialized as [T].
     */
    @Suppress("UNCHECKED_CAST")
    fun <U : T> upcastTo(): Expression<U> = this as Expression<U>

    /**
     * Re-types the expression to the parent type [D].
     *
     * Unchecked: returns the same instance, so [getType] keeps reporting the original type. The
     * `T : D` bound constrains nothing — that `T` shadows the interface's own type parameter.
     */
    @Suppress("UNCHECKED_CAST")
    fun <D, T : D> downcast(): Expression<D> = this as Expression<D>

    /**
     * Re-types the expression to any [U], without a bound.
     *
     * Unchecked: returns the same instance, so [getType] keeps reporting the original type.
     */
    @Suppress("UNCHECKED_CAST")
    fun <U> forceCastTo(): Expression<U> = this as Expression<U>

    /**
     * Re-types the expression to `T?`.
     *
     * Unchecked: returns the same instance, so [getType] still reports the non-nullable [T] and
     * un-serializing a null result fails.
     */
    @Suppress("UNCHECKED_CAST")
    fun nullable(): Expression<T?> = this as Expression<T?>
}
