@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.lang.VaultFunctionMarker

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the absolute part of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#abs
 */
@VaultFunctionMarker val ABS = aqlFunc<Number>("ABS")

/** Return the absolute part of value. */
@VaultFunctionMarker
inline fun <reified T : Number> ABS(value: AqlExpression<T>): AqlExpression<T> = ABS.call(type = kType(), value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the arccosine of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#acos
 */
@VaultFunctionMarker val ACOS = aqlFunc<Number?>("ACOS")

/** Return the arccosine of value. */
@VaultFunctionMarker
fun <T : Number> ACOS(value: AqlExpression<T>): AqlExpression<Number?> = ACOS.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Add all elements of an array to another array. All values are added at the end of the array (right side).
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#append
 */
@VaultFunctionMarker val APPEND = aqlFunc<List<Any>>("APPEND")

/** Add all elements of an array to another array. All values are added at the end of the array (right side). */
@VaultFunctionMarker
inline fun <reified T> APPEND(
    anyArray: AqlExpression<out List<T>>, values: AqlExpression<out List<T>>,
): AqlExpression<List<T>> =
    APPEND.call(type = kType(), anyArray, values)

/** Add all elements of an array to another array. All values are added at the end of the array (right side). */
@VaultFunctionMarker
inline fun <reified T> APPEND(
    anyArray: AqlExpression<out List<T>>, values: AqlExpression<out List<T>>, unique: AqlExpression<Boolean>,
): AqlExpression<List<T>> =
    APPEND.call(type = kType(), anyArray, values, unique)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return whether search is contained in anyArray.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#contains_array
 *
 * To get the position of the occurrence, use CONTAINS_ARRAY_IDX()
 */
@VaultFunctionMarker val CONTAINS_ARRAY = aqlFunc<Boolean>("CONTAINS_ARRAY")

/** Return whether [search] is contained in [anyArray]. */
@VaultFunctionMarker
fun <T> CONTAINS_ARRAY(
    anyArray: AqlExpression<out Collection<T>>, search: AqlExpression<T>,
): AqlExpression<Boolean> =
    CONTAINS_ARRAY.call(anyArray, search)

/** Return the position of the match (starting with 0) otherwise -1. */
@VaultFunctionMarker fun <T> CONTAINS_ARRAY_IDX(
    anyArray: AqlExpression<List<T>>, search: AqlExpression<T>,
): AqlExpression<Number> =
    CONTAINS_ARRAY.call(type = kType(), anyArray, search, true.aql)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Determine the character length of a string.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#count
 * see https://docs.arangodb.com/current/AQL/Functions/Array.html#count
 */
@VaultFunctionMarker val COUNT = aqlFunc<Int>("COUNT")

/** Determine the character length of a string. */
@VaultFunctionMarker @JvmName("COUNT_String")
fun COUNT(expr: AqlExpression<String>): AqlExpression<Int> = COUNT.call(expr)

/** Determine the number of elements in an array. */
@VaultFunctionMarker @JvmName("COUNT_Array")
fun <T> COUNT(expr: AqlExpression<out Collection<T>>): AqlExpression<Int> = COUNT.call(expr)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Determine the number of distinct elements in an array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#count
 */
@VaultFunctionMarker val COUNT_DISTINCT = aqlFunc<Int>("COUNT_DISTINCT")

/** Determine the number of distinct elements in an array. */
@VaultFunctionMarker
fun <T> COUNT_DISTINCT(anyArray: AqlExpression<out Collection<T>>): AqlExpression<Int> = COUNT_DISTINCT.call(anyArray)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Determine the number of distinct elements in an array.
 *
 * Alias of COUNT_DISTINCT
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#count
 */
@VaultFunctionMarker val COUNT_UNIQUE = aqlFunc<Int>("COUNT_UNIQUE")

@VaultFunctionMarker
fun <T> COUNT_UNIQUE(anyArray: AqlExpression<out Collection<T>>): AqlExpression<Int> = COUNT_UNIQUE.call(anyArray)
