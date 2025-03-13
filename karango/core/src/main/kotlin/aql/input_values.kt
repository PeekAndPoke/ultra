@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.vault.lang.Expression
import de.peekandpoke.ultra.vault.lang.VaultInputValueMarker
import de.peekandpoke.ultra.vault.lang.expr

/**
 * Guard function to prevent calls to .aql() on existing Expressions
 */
@Suppress("unused", "UnusedReceiverParameter")
@Deprecated("Cannot use .aql() on existing Expressions", level = DeprecationLevel.ERROR)
@VaultInputValueMarker
fun <T> Expression<T>.aql(): Nothing {
    throw Exception("Cannot call .aql() on already existing Expression")
}

/**
 * Guard val to prevent calls to .aql on existing Expressions
 */
@Suppress("unused", "UnusedReceiverParameter")
@Deprecated("Cannot use .aql() on existing Expressions", level = DeprecationLevel.ERROR)
@VaultInputValueMarker
val <T> Expression<T>.aql: Nothing
    get() {
        throw Exception("Cannot call .aql() on already existing Expression")
    }

/**
 * Helper to make any object an AQL expression (forwards to Vault's [expr])
 *
 * @see [expr] in Vault for details
 *
 * Usage:
 *
 * 1.aql("name-in-query")
 *
 * "string".aql()
 *
 * true.aql()
 *
 * Obj().aql()
 */
@VaultInputValueMarker
inline fun <reified T> T.aql(name: String = "v"): Expression<T> = this.expr(name)

/**
 * Shorthand for converting any object into an AQL expression without specifying a name (forwards to Vault's [expr])
 *
 * @see [expr] in Vault for details
 *
 * Usage:
 *
 * 1.aql
 *
 * "string".aql
 *
 * true.aql
 *
 * Obj().aql
 */
@VaultInputValueMarker
inline val <reified T> T.aql: Expression<T>
    get() = this.aql()

/**
 * Helper to make a "null" AQL expression (forwards to Vault's [expr])
 *
 * @see [expr] in Vault for details
 *
 * Usage:
 *
 * null.aql()
 *
 * null.aql("some name")
 */
@Suppress("unused")
@VaultInputValueMarker
fun Nothing?.aql(name: String = "v"): Expression<Any?> = this.expr(name)

/**
 * Shorthand for converting a "null" into an AQL expression without specifying a name (forwards to Vault's [expr])
 *
 * @see [expr] in Vault for details
 *
 * Usage:
 *
 * null.aql
 */
@VaultInputValueMarker
val Nothing?.aql: Expression<Any?>
    get() = this.aql()
