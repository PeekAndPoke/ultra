@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kListType
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.lang.VaultInputValueMarker

/**
 * Guard function to prevent calls to .aql() on existing AqlExpressions
 */
@Suppress("unused", "UnusedReceiverParameter")
@Deprecated("Cannot use .aql() on existing AqlExpressions", level = DeprecationLevel.ERROR)
@VaultInputValueMarker
fun <T> AqlExpression<T>.aql(): Nothing {
    throw Exception("Cannot call .aql() on already existing AqlExpression")
}

/**
 * Guard val to prevent calls to .aql on existing AqlExpressions
 */
@Suppress("unused", "UnusedReceiverParameter")
@Deprecated("Cannot use .aql() on existing AqlExpressions", level = DeprecationLevel.ERROR)
@VaultInputValueMarker
val <T> AqlExpression<T>.aql: Nothing
    get() {
        throw Exception("Cannot call .aql() on already existing AqlExpression")
    }


/**
 * Helper to make any object am aql expression
 */
@Suppress("UNCHECKED_CAST")
@VaultInputValueMarker
inline fun <reified T> T.aql(name: String = "v"): AqlExpression<T> = when (this) {
    // guard, so we do not wrap an AqlExpression again
    is AqlExpression<*> -> this as AqlExpression<T>
    // otherwise we create a value expression
    else -> AqlValueExpr(kType(), this, name)
}

/**
 * Helper to make any object am aql expression
 */
@Suppress("UNCHECKED_CAST")
fun <T> T.aql(type: TypeRef<T>, name: String = "v"): AqlExpression<T> = when (this) {
    // guard, so we do not wrap an AqlExpression again
    is AqlExpression<*> -> this as AqlExpression<T>
    // otherwise we create a value expression
    else -> AqlValueExpr(type, this, name)
}

/**
 * Shorthand for converting any object into an AQL expression without specifying a name (forwards to Vault's [aql])
 *
 * @see [aql] in Vault for details
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
inline val <reified T> T.aql: AqlExpression<T>
    get() = this.aql()

/**
 * Shorthand for converting a "null" into an AQL expression without specifying a name (forwards to Vault's [aql])
 *
 * @see [aql] in Vault for details
 *
 * Usage:
 *
 * null.aql
 */
@VaultInputValueMarker
val Nothing?.aql: AqlExpression<Any?>
    get() = this.aql()

@VaultInputValueMarker
inline fun <reified T> ARRAY(vararg args: AqlExpression<out T>): AqlExpression<List<T>> =
    ARRAY(args.toList())

@VaultInputValueMarker
inline fun <reified T> ARRAY(args: List<AqlExpression<out T>>): AqlExpression<List<T>> =
    AqlArrayValueExpr(kListType(), args)

@VaultInputValueMarker
@JvmName("OBJECT_Pair_StringExpr")
inline fun <reified T> OBJECT(
    vararg pairs: Pair<String, AqlExpression<out T>>,
): AqlObjectValueExpr<T> {
    val mapped: List<Pair<AqlExpression<String>, AqlExpression<out T>>> =
        pairs.map { (k: String, v: AqlExpression<out T>) -> k.aql to v }.toList()

    return OBJECT(mapped)
}

@VaultInputValueMarker
@JvmName("OBJECT_Pair_ExprExpr")
inline fun <reified T> OBJECT(
    vararg pairs: Pair<AqlExpression<String>, AqlExpression<out T>>,
): AqlObjectValueExpr<T> =
    OBJECT(pairs.toList())

@VaultInputValueMarker
inline fun <reified T> OBJECT(
    pairs: List<Pair<AqlExpression<String>, AqlExpression<out T>>>,
): AqlObjectValueExpr<T> =
    AqlObjectValueExpr(kType(), pairs)
