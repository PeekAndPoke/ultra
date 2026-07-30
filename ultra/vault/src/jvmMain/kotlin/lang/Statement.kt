package io.peekandpoke.ultra.vault.lang

import io.peekandpoke.ultra.reflection.TypeRef

/**
 * Base interface for all Statements.
 *
 * Statements return nothing and belong in a statement list, not nested inside an [Expression].
 * Nothing enforces that: a Statement is itself an `Expression<Unit?>`, so the DSL accepts one
 * wherever an expression is expected and the backend then emits unparsable output.
 */
interface Statement : Expression<Unit?> {
    /** A statement has no result, so its type is always `Unit?`. */
    override fun getType(): TypeRef<Unit?> = TypeRef.UnitNull
}
