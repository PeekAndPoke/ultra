package io.peekandpoke.ultra.vault.lang

import io.peekandpoke.ultra.reflection.TypeRef

/**
 * Base interface for all Statements.
 *
 * Statements return nothing. Statements cannot be nested within Expressions.
 */
interface Statement : Expression<Unit?> {
    override fun getType(): TypeRef<Unit?> = TypeRef.UnitNull
}
