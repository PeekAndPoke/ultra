package de.peekandpoke.ultra.vault.lang

import de.peekandpoke.ultra.common.reflection.TypeRef

/**
 * Base interface for all Statements.
 *
 * Statements return nothing. Statements cannot be nested within Expressions.
 */
interface Statement : Expression<Unit?> {
    override fun getType(): TypeRef<Unit?> = TypeRef.UnitNull
}
