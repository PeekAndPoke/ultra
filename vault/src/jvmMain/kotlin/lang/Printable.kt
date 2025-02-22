package de.peekandpoke.ultra.vault.lang

/**
 * Marks a class as printable by a [Printer]
 */
interface Printable {
    fun print(p: Printer): Any
}
