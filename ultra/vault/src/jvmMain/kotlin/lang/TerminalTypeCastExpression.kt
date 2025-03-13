package de.peekandpoke.ultra.vault.lang

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.unList

/**
 * Internal expression representing a type cast
 */
class TerminalTypeCastExpression<T>(private val type: TypeRef<List<T>>, private val wrapped: TerminalExpr<*>) :
    TerminalExpr<T> {

    override fun innerType(): TypeRef<T> = type.unList

    override fun getType() = type

    override fun print(p: Printer) = wrapped.print(p)
}
