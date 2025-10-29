@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.lang.VaultFunctionMarker

/**
 * Remove the first element of array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#shift
 */
@VaultFunctionMarker val SHIFT = aqlFunc<List<Any?>>("SHIFT")

@VaultFunctionMarker
inline fun <reified T> SHIFT(anyArray: AqlExpression<List<T>>): AqlExpression<List<T>> =
    SHIFT.call(type = kType(), anyArray)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Extract a slice of anyArray.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#slice
 */
@VaultFunctionMarker val SLICE = aqlFunc<List<Any?>>("SLICE")

/** Extract a slice of anyArray. */
@VaultFunctionMarker
inline fun <reified T, N : Number> SLICE(
    anyArray: AqlExpression<List<T>>,
    start: AqlExpression<N>,
): AqlExpression<List<T>> =
    SLICE.call(type = kType(), anyArray, start)

/** Extract a slice of anyArray. */
@VaultFunctionMarker
inline fun <reified T, NS : Number, NL : Number> SLICE(
    anyArray: AqlExpression<List<T>>,
    start: AqlExpression<NS>,
    length: AqlExpression<NL>,
): AqlExpression<List<T>> =
    SLICE.call(type = kType(), anyArray, start, length)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Sort all elements in anyArray. The function will use the default comparison order for AQL value types.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#sorted
 */
@VaultFunctionMarker val SORTED = aqlFunc<List<Any?>>("SORTED")

/** Sort all elements in anyArray. The function will use the default comparison order for AQL value types. */
@VaultFunctionMarker
inline fun <reified T> SORTED(anyArray: AqlExpression<List<T>>): AqlExpression<List<T>> =
    SORTED.call(type = kType(), anyArray)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Sort all elements in anyArray and remove duplicates.
 *
 * The function will use the default comparison order for AQL value types.
 * Additionally, the values in the result array will be made unique.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#sorted
 */
@VaultFunctionMarker val SORTED_UNIQUE = aqlFunc<List<Any?>>("SORTED_UNIQUE")

/** Sort all elements in anyArray and remove duplicates. */
@VaultFunctionMarker
inline fun <reified T> SORTED_UNIQUE(anyArray: AqlExpression<List<T>>): AqlExpression<List<T>> =
    SORTED_UNIQUE.call(type = kType(), anyArray)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the union of all arrays specified.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#union
 */
@VaultFunctionMarker val UNION = aqlFunc<List<Any?>>("UNION")

/** Return the union of all arrays specified. */
@VaultFunctionMarker
inline fun <reified T : Any> UNION(
    array1: AqlExpression<out List<T>>,
    array2: AqlExpression<out List<T>>,
    vararg arrayN: AqlExpression<out List<T>>,
): AqlExpression<List<T>> =
    UNION.call(type = kType(), array1, array2, *arrayN)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the union of distinct values of all arrays specified.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#union_destinct
 */
@VaultFunctionMarker val UNION_DISTINCT = aqlFunc<List<Any?>>("UNION_DISTINCT")

/** Return the union of distinct values of all arrays specified. */
@VaultFunctionMarker
inline fun <reified T : Any> UNION_DISTINCT(
    array1: AqlExpression<out List<T>>,
    array2: AqlExpression<out List<T>>,
    vararg arrayN: AqlExpression<out List<T>>,
): AqlExpression<List<T>> =
    UNION_DISTINCT.call(type = kType(), array1, array2, *arrayN)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return all unique elements in anyArray. To determine uniqueness, the function will use the comparison order.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#unique
 */
@VaultFunctionMarker val UNIQUE = aqlFunc<List<Any?>>("UNIQUE")

/** Return all unique elements in anyArray. To determine uniqueness, the function will use the comparison order. */
@VaultFunctionMarker
inline fun <reified T> UNIQUE(anyArray: AqlExpression<List<T>>): AqlExpression<List<T>> =
    UNIQUE.call(type = kType(), anyArray)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@VaultFunctionMarker val UNSET = aqlFunc<Any>("UNSET")

@VaultFunctionMarker
inline fun <reified T> UNSET(
    document: AqlExpression<out T>,
    attributeName1: AqlExpression<String>,
    vararg attributeNameN: AqlExpression<String>,
): AqlExpression<T> =
    UNSET.call(type = kType(), document, attributeName1, *attributeNameN)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Prepend value to anyArray (left side).
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#unshift
 */
@VaultFunctionMarker val UNSHIFT = aqlFunc<List<Any?>>("UNSHIFT")

/** Prepend value to anyArray (left side). */
@VaultFunctionMarker
inline fun <reified T> UNSHIFT(
    anyArray: AqlExpression<out List<T>>,
    value: AqlExpression<out T>,
): AqlExpression<List<T>> =
    UNSHIFT.call(type = kType(), anyArray, value)

/** Prepend value to anyArray (left side). */
@VaultFunctionMarker
inline fun <reified T> UNSHIFT(
    anyArray: AqlExpression<out List<T>>,
    value: AqlExpression<out T>,
    unique: AqlExpression<Boolean>,
): AqlExpression<List<T>> =
    UNSHIFT.call(type = kType(), anyArray, value, unique)
