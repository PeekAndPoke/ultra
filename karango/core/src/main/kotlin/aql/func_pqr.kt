@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.lang.VaultFunctionMarker

/**
 * Remove the last element of array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#pop
 */
@VaultFunctionMarker val POP = aqlFunc<List<Any?>>("POP")

/** Remove the last element of array. */
@VaultFunctionMarker
inline fun <reified T> POP(anyArray: AqlExpression<List<T>>): AqlExpression<List<T>> =
    POP.call(type = kType(), anyArray)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return whether search is contained in array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#position
 *
 * To get the position of the occurrence use CONTAINS_ARRAY_IDX()
 */
@VaultFunctionMarker val POSITION = aqlFunc<Number>("POSITION")

/** Return whether [search] is contained in [anyArray]. */
@VaultFunctionMarker
fun <T> POSITION(anyArray: AqlExpression<List<T>>, search: AqlExpression<T>): AqlExpression<Number> =
    POSITION.call(anyArray, search, true.aql)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Append value to anyArray (right side).
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#push
 */
@VaultFunctionMarker val PUSH = aqlFunc<List<Any?>>("PUSH")

/** Append value to anyArray (right side). */
@VaultFunctionMarker
inline fun <reified T> PUSH(anyArray: AqlExpression<out List<T>>, value: AqlExpression<out T>): AqlExpression<List<T>> =
    PUSH.call(type = kType(), anyArray, value)

/** Remove the last element of array. */
@VaultFunctionMarker
inline fun <reified T> PUSH(
    anyArray: AqlExpression<out List<T>>,
    value: AqlExpression<out T>,
    unique: AqlExpression<Boolean>,
): AqlExpression<List<T>> =
    PUSH.call(type = kType(), anyArray, value, unique)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Remove the element at position from the anyArray.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#remove_nth
 */
@VaultFunctionMarker val REMOVE_NTH = aqlFunc<List<Any?>>("REMOVE_NTH")

/** Remove the element at position from the anyArray. */
@VaultFunctionMarker
inline fun <reified T, N : Number> REMOVE_NTH(
    anyArray: AqlExpression<List<T>>,
    position: AqlExpression<N>,
): AqlExpression<List<T>> =
    REMOVE_NTH.call(type = kType(), anyArray, position)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Remove the element at value from the anyArray.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#remove_value
 */
@VaultFunctionMarker val REMOVE_VALUE = aqlFunc<List<Any?>>("REMOVE_VALUE")

/** Remove the element at value from the anyArray. */
@VaultFunctionMarker
inline fun <reified T> REMOVE_VALUE(anyArray: AqlExpression<List<T>>, value: AqlExpression<T>): AqlExpression<List<T>> =
    REMOVE_VALUE.call(type = kType(), anyArray, value)

/** Remove the element at value from the anyArray. */
@VaultFunctionMarker
fun <T, N : Number> REMOVE_VALUE(
    anyArray: AqlExpression<List<T>>,
    value: AqlExpression<T>,
    limit: AqlExpression<N>,
): AqlExpression<List<T>> =
    REMOVE_VALUE.call(type = anyArray.getType(), anyArray, value, limit)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Remove all occurrences of any of the values from anyArray.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#remove_values
 */
@VaultFunctionMarker val REMOVE_VALUES = aqlFunc<List<Any?>>("REMOVE_VALUES")

/** Remove all occurrences of any of the values from anyArray. */
@VaultFunctionMarker
inline fun <reified T> REMOVE_VALUES(
    anyArray: AqlExpression<out List<T>>,
    values: AqlExpression<out List<T>>,
): AqlExpression<List<T>> =
    REMOVE_VALUES.call(type = kType(), anyArray, values)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return an array with its elements reversed.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#reverse
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#reverse
 */
@VaultFunctionMarker val REVERSE = aqlFunc<Any>("REVERSE")

/** Return an array with its elements reversed. */
@VaultFunctionMarker @JvmName("REVERSE_Array")
inline fun <reified T> REVERSE(anyArray: AqlExpression<List<T>>): AqlExpression<List<T>> =
    REVERSE.call(type = kType(), anyArray)

/** Return the reverse of the string value. */
@VaultFunctionMarker @JvmName("REVERSE_String")
fun REVERSE_STRING(value: AqlExpression<String>): AqlExpression<String> =
    REVERSE.call(type = kType(), value)
