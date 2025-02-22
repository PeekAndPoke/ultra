package de.peekandpoke.ultra.vault.lang

import de.peekandpoke.ultra.common.reflection.TypeRef

/**
 * Internal expression representing a type cast
 */
class TypeCastExpression<T>(private val type: TypeRef<T>, private val wrapped: Expression<*>) : Expression<T> {

    override fun getType() = type

    override fun print(p: Printer) = wrapped.print(p)
}

