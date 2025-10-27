package de.peekandpoke.ultra.vault.lang

import de.peekandpoke.ultra.common.reflection.TypeRef

/**
 * Base interface for all Expressions.
 */
interface Expression<T> {
    /**
     * Returns a reference to the type that the expression represents.
     *
     * The type information is needed for un-serializing a query result.
     */
    fun getType(): TypeRef<T>

    /**
     * Up-casts the expression of type [T] to the child type [U]
     */
    @Suppress("UNCHECKED_CAST")
    fun <U : T> upcastTo(): Expression<U> = this as Expression<U>

    /**
     * Down-casts the expression of type [T] to the parent type [D]
     */
    @Suppress("UNCHECKED_CAST")
    fun <D, T : D> downcast(): Expression<D> = this as Expression<D>

    /**
     * Casts the expression
     */
    @Suppress("UNCHECKED_CAST")
    fun <U> forceCastTo(): Expression<U> = this as Expression<U>

    /**
     * Makes the expression nullable
     */
    @Suppress("UNCHECKED_CAST")
    fun nullable(): Expression<T?> = this as Expression<T?>
}
