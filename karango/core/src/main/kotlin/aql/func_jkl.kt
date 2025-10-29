@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.lang.VaultFunctionMarker

/**
 * Get the last element of an array. It is the same as anyArray[-1].
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#last
 */
@VaultFunctionMarker val LAST = aqlFunc<Any?>("LAST")

/** Get the last element of an array. It is the same as anyArray[-1]. */
@VaultFunctionMarker
inline fun <reified T> LAST(anyArray: AqlExpression<List<T>>): AqlExpression<T?> =
    LAST.call(type = kType(), anyArray)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Determine the number of elements in an array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#length
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#length
 */
@VaultFunctionMarker val LENGTH = aqlFunc<Number>("LENGTH")

/** Determine the number of elements in an array. */
@VaultFunctionMarker @JvmName("LENGTH_Array")
fun <T> LENGTH(anyArray: AqlExpression<List<T>>): AqlExpression<Number> = LENGTH.call(anyArray)

/** Determine the character length of a string. */
@VaultFunctionMarker @JvmName("LENGTH_String")
fun LENGTH(expr: AqlExpression<String>): AqlExpression<Number> = LENGTH.call(expr)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the natural logarithm of value. The base is Euler's constant (2.71828...).
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#log
 */
@VaultFunctionMarker val LOG = aqlFunc<Number?>("LOG")

/** Return the natural logarithm of value. The base is Euler's constant (2.71828...). */
@VaultFunctionMarker
fun <T : Number> LOG(value: AqlExpression<T>): AqlExpression<Number?> = LOG.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the base 2 logarithm of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#log
 */
@VaultFunctionMarker val LOG2 = aqlFunc<Number?>("LOG2")

/** Return the base 2 logarithm of value. */
@VaultFunctionMarker
fun <T : Number> LOG2(value: AqlExpression<T>): AqlExpression<Number?> = LOG2.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the base 10 logarithm of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#log
 */
@VaultFunctionMarker val LOG10 = aqlFunc<Number?>("LOG10")

/** Return the base 10 logarithm of value. */
@VaultFunctionMarker
fun <T : Number> LOG10(value: AqlExpression<T>): AqlExpression<Number?> = LOG10.call(value)
