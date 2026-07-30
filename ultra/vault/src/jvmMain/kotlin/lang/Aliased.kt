package io.peekandpoke.ultra.vault.lang

/**
 * Helper interface for the QueryPrinter.
 *
 * When an Expression has this interface query printers will use the value returned by getAlias()
 */
interface Aliased {
    /**
     * The name a query printer should use for this expression, e.g. as a bind-parameter name.
     *
     * Need not be unique within a query — printers uniquify it before emitting.
     */
    fun getAlias(): String
}
