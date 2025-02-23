package de.peekandpoke.ultra.vault.lang

/**
 * Helper interface for the QueryPrinter.
 *
 * When an Expression has this interface query printers will use the value returned by getAlias()
 */
interface Aliased {
    fun getAlias(): String
}
