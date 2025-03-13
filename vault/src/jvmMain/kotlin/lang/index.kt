@file:Suppress("FunctionName")

package de.peekandpoke.ultra.vault.lang

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kListType
import de.peekandpoke.ultra.common.reflection.kType

// //  DSL annotations  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@DslMarker
annotation class VaultDslMarker

@DslMarker
annotation class VaultFunctionMarker

@DslMarker
annotation class VaultTerminalExpressionMarker

@DslMarker
annotation class VaultInputValueMarker

@DslMarker
annotation class VaultTypeConversionMarker

// //  Convenience type aliases  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

/** Type alias for a simple list */
typealias L1<T> = List<T>

/** Type alias for a list of lists */
typealias L2<T> = List<List<T>>

/** Type alias for a list of lists of lists */
typealias L3<T> = List<List<List<T>>>

/** Type alias for a list of lists of lists of lists */
typealias L4<T> = List<List<List<List<T>>>>

/** Type alias for a list of lists of lists of lists of lists*/
typealias L5<T> = List<List<List<List<List<T>>>>>

// //  Input values  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@VaultInputValueMarker
inline fun <reified T> ARRAY(vararg args: Expression<out T>): Expression<List<T>> = ARRAY(args.toList())

@VaultInputValueMarker
inline fun <reified T> ARRAY(args: List<Expression<out T>>): Expression<List<T>> = ArrayValueExpr(kListType(), args)

@VaultInputValueMarker
@JvmName("OBJECT_Pair_StringExpr")
inline fun <reified T> OBJECT(vararg pairs: Pair<String, Expression<out T>>): ObjectValueExpr<T> = OBJECT(
    pairs.map { (k, v) -> k.expr to v }.toList()
)

@VaultInputValueMarker
@JvmName("OBJECT_Pair_ExprExpr")
inline fun <reified T> OBJECT(vararg pairs: Pair<Expression<String>, Expression<out T>>): ObjectValueExpr<T> =
    OBJECT(pairs.toList())

@VaultInputValueMarker
inline fun <reified T> OBJECT(pairs: List<Pair<Expression<String>, Expression<out T>>>): ObjectValueExpr<T> =
    ObjectValueExpr(kType(), pairs)

/**
 * Guard function to prevent calls to .expr() on existing Expressions
 */
@Suppress("unused", "UnusedReceiverParameter")
@Deprecated("Cannot use .expr() on an Expressions", level = DeprecationLevel.ERROR)
@VaultInputValueMarker
fun <T> Expression<T>.expr(): Nothing {
    throw Exception("Cannot call .expr() on an Expression")
}

/**
 * Guard val to prevent calls to .expr on existing Expressions
 */
@Suppress("unused", "UnusedReceiverParameter")
@Deprecated("Cannot use .expr on an Expressions", level = DeprecationLevel.ERROR)
@VaultInputValueMarker
val <T> Expression<T>.expr: Nothing
    get() {
        throw Exception("Cannot call .expr on an Expression")
    }

/**
 * Helper to make any object an Vault expression
 *
 * Usage:
 *
 * 1.expr() // default name "v" will be used
 * 1.expr("name-in-query")
 *
 * "string".expr("name-in-query")
 *
 * true.expr("name-in-query")
 *
 * Obj().expr("name-in-query")
 */
@Suppress("UNCHECKED_CAST")
@VaultInputValueMarker
inline fun <reified T> T.expr(name: String = "v"): Expression<T> = when (this) {
    // guard, so we do not wrap an Expression again
    is Expression<*> -> this as Expression<T>
    // otherwise we create a value expression
    else -> ValueExpr(kType(), this, name)
}

@Suppress("UNCHECKED_CAST")
fun <T> T.expr(type: TypeRef<T>, name: String = "v"): Expression<T> = when (this) {
    // guard, so we do not wrap an Expression again
    is Expression<*> -> this as Expression<T>
    // otherwise we create a value expression
    else -> ValueExpr(type, this, name)
}

/**
 * Shorthand for converting any object into an Vault expression without specifying a name
 *
 * Usage:
 *
 * 1.expr
 *
 * "string".expr
 *
 * true.expr
 *
 * Obj().expr
 */
@VaultInputValueMarker
inline val <reified T> T.expr: Expression<T>
    get() = this.expr()

/**
 * Helper to make a "null" Vault expression
 *
 * Usage:
 *
 * null.expr()
 *
 * null.expr("name-in-query")
 */
@Suppress("unused")
@VaultInputValueMarker
fun Nothing?.expr(name: String = "v"): Expression<Any?> = NullValueExpr(name)

/**
 * Shorthand for converting a "null" into an Vault expression without specifying a name
 *
 * Usage:
 *
 * null.expr
 */
@VaultInputValueMarker
val Nothing?.expr: Expression<Any?>
    get() = this.expr()
