@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.kListType
import de.peekandpoke.ultra.common.reflection.unList
import de.peekandpoke.ultra.vault.lang.VaultFunctionMarker

/**
 * Add all elements of an array to another array. All values are added at the end of the array (right side).
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#append
 */
@VaultFunctionMarker
inline fun <reified T> APPEND(
    anyArray: AqlExpression<out List<T>>,
    values: AqlExpression<out List<T>>,
): AqlExpression<List<T>> =
    AqlFunc.APPEND.arrayCall(type = kListType<T>(), anyArray, values)

/**
 * Return the population variance of the values in array.
 *
 * Alias for VARIANCE_POPULATION
 *
 * https://docs.arangodb.com/current/AQL/Functions/Array.html#append
 */
@VaultFunctionMarker
inline fun <reified T> APPEND(
    anyArray: AqlExpression<out List<T>>,
    values: AqlExpression<out List<T>>,
    unique: AqlExpression<Boolean>,
): AqlExpression<List<T>> =
    AqlFunc.APPEND.arrayCall(type = kListType<T>(), anyArray, values, unique)

/**
 * Return whether [search] is contained in [anyArray].
 *
 * Alias of POSITION
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#contains_array
 *
 * To get the position of the occurrence, use CONTAINS_ARRAY_IDX()
 */
@VaultFunctionMarker
fun <T> CONTAINS_ARRAY(
    anyArray: AqlExpression<List<T>>,
    search: AqlExpression<T>,
): AqlExpression<Boolean> =
    AqlFunc.CONTAINS_ARRAY.boolCall(anyArray, search)

/**
 * Return whether [search] is contained in [anyArray].
 *
 * Alias of POSITION
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#contains_array
 *
 * To get the position of the occurrence, use CONTAINS_ARRAY_IDX()
 */
@JvmName("CONTAINER_ARRAY_SET")
@VaultFunctionMarker
fun <T> CONTAINS_ARRAY(
    anyArray: AqlExpression<Set<T>>,
    search: AqlExpression<T>,
): AqlExpression<Boolean> =
    AqlFunc.CONTAINS_ARRAY.boolCall(anyArray, search)

/**
 * Return the position of the match (starting with 0) otherwise -1.
 *
 * Alias of POSITION
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#contains_array
 *
 * To get the position of the occurrence use CONTAINS_ARRAY_IDX()
 */
@VaultFunctionMarker
fun <T> CONTAINS_ARRAY_IDX(
    anyArray: AqlExpression<List<T>>,
    search: AqlExpression<T>,
): AqlExpression<Number> =
    AqlFunc.CONTAINS_ARRAY.numberCall(anyArray, search, true.aql)

/**
 * Determine the number of elements in an array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#count
 */
@VaultFunctionMarker
@JvmName("COUNT_Set")
fun <T> COUNT(
    anyArray: AqlExpression<Set<T>>,
): AqlExpression<Number> =
    AqlFunc.COUNT.numberCall(anyArray)

/**
 * Determine the number of elements in an array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#count
 */
@VaultFunctionMarker
@JvmName("COUNT_List")
fun <T> COUNT(
    anyArray: AqlExpression<List<T>>,
): AqlExpression<Number> =
    AqlFunc.COUNT.numberCall(anyArray)

/**
 * Determine the number of distinct elements in an array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#count
 */
@VaultFunctionMarker
fun <T> COUNT_DISTINCT(
    anyArray: AqlExpression<List<T>>,
): AqlExpression<Number> =
    AqlFunc.COUNT_DISTINCT.numberCall(anyArray)

/**
 * Determine the number of distinct elements in an array.
 *
 * Alias of COUNT_DISTINCT
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#count
 */
@VaultFunctionMarker
fun <T> COUNT_UNIQUE(
    anyArray: AqlExpression<List<T>>,
): AqlExpression<Number> =
    AqlFunc.COUNT_UNIQUE.numberCall(anyArray)

/**
 * Get the first element of an array. It is the same as anyArray[0].
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#first
 */
@VaultFunctionMarker
fun <T> FIRST(
    anyArray: AqlExpression<List<T>>,
): AqlExpression<T?> =
    AqlFunc.FIRST.nullableCall(type = anyArray.getType().unList.nullable, anyArray)

/**
 * Turn an array of arrays into a flat array. All array elements in array will be expanded in the result array.
 * Non-array elements are added as they are. The function will recurse into sub-arrays up to the specified depth.
 * Duplicates will not be removed.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#flatten
 */
@VaultFunctionMarker
fun <T> FLATTEN(
    anyArray: AqlExpression<List<T>>,
): AqlExpression<List<Any?>> =
    AqlFunc.FLATTEN.arrayCall(type = kListType<Any?>(), anyArray)

/**
 * Turn an array of arrays into a flat array. All array elements in array will be expanded in the result array.
 * Non-array elements are added as they are. The function will recurse into sub-arrays up to the specified depth.
 * Duplicates will not be removed.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#flatten
 */
@VaultFunctionMarker
fun <T, N : Number> FLATTEN(
    anyArray: AqlExpression<List<T>>,
    depth: AqlExpression<N>,
): AqlExpression<List<Any?>> =
    AqlFunc.FLATTEN.arrayCall(type = kListType<Any?>(), anyArray, depth)

/**
 * Return the intersection of all arrays specified. The result is an array of values that occur in all arguments.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#intersection
 */
@VaultFunctionMarker
inline fun <reified T : Any> INTERSECTION(
    array1: AqlExpression<out Collection<T>>,
    array2: AqlExpression<out Collection<T>>,
    vararg arrayN: AqlExpression<out Collection<T>>,
): AqlExpression<List<T>> =
    AqlFunc.INTERSECTION.arrayCall(type = kListType<T>(), array1, array2, *arrayN)

/**
 * Get the last element of an array. It is the same as anyArray[-1].
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#last
 */
@VaultFunctionMarker
fun <T> LAST(
    anyArray: AqlExpression<List<T>>,
): AqlExpression<T?> =
    AqlFunc.LAST.nullableCall(type = anyArray.getType().unList.nullable, anyArray)

/**
 * Determine the number of elements in an array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#length
 */
@VaultFunctionMarker
fun <T> LENGTH(
    anyArray: AqlExpression<List<T>>,
): AqlExpression<Number> =
    AqlFunc.LENGTH.numberCall(anyArray)

/**
 * Return the difference of all arrays specified.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#minus
 */
@VaultFunctionMarker
inline fun <reified T : Any> MINUS(
    array1: AqlExpression<out List<T>>,
    array2: AqlExpression<out List<T>>,
    vararg arrayN: AqlExpression<out List<T>>,
): AqlExpression<List<T>> =
    AqlFunc.MINUS.arrayCall(type = kListType<T>(), array1, array2, *arrayN)

/**
 * Get the element of an array at a given position. It is the same as anyArray[position] for positive positions, but does not support negative positions.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#nth
 */
@VaultFunctionMarker
fun <T, N : Number> NTH(
    anyArray: AqlExpression<List<T>>,
    position: AqlExpression<N>,
): AqlExpression<T?> =
    AqlFunc.NTH.nullableCall(type = anyArray.getType().unList.nullable, anyArray, position)

/**
 * Return the values that occur only once across all arrays specified.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#outersection
 */
@VaultFunctionMarker
inline fun <reified T : Any> OUTERSECTION(
    array1: AqlExpression<out List<T>>,
    array2: AqlExpression<out List<T>>,
    vararg arrayN: AqlExpression<out List<T>>,
): AqlExpression<List<T>> =
    AqlFunc.OUTERSECTION.arrayCall(type = kListType<T>(), array1, array2, *arrayN)

/**
 * Remove the last element of array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#pop
 */
@VaultFunctionMarker
fun <T> POP(
    anyArray: AqlExpression<List<T>>,
): AqlExpression<List<T>> =
    AqlFunc.POP.arrayCall(type = anyArray.getType(), anyArray)

/**
 * Return whether search is contained in array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#position
 *
 * To get the position of the occurrence use CONTAINS_ARRAY_IDX()
 */
@VaultFunctionMarker
fun <T> POSITION(
    anyArray: AqlExpression<List<T>>,
    search: AqlExpression<T>,
): AqlExpression<Boolean> =
    AqlFunc.POSITION.boolCall(anyArray, search)

/**
 * Return the position of the match (starting with 0) otherwise -1.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#position
 *
 * To get the position of the occurrence use CONTAINS_ARRAY_IDX()
 */
@VaultFunctionMarker
fun <T> POSITION_IDX(
    anyArray: AqlExpression<List<T>>,
    search: AqlExpression<T>,
): AqlExpression<Number> =
    AqlFunc.POSITION.numberCall(anyArray, search, true.aql)

/**
 * Append value to anyArray (right side).
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#push
 */
@VaultFunctionMarker
inline fun <reified T> PUSH(
    anyArray: AqlExpression<out List<T>>,
    value: AqlExpression<out T>,
): AqlExpression<List<T>> =
    AqlFunc.PUSH.arrayCall(type = kListType<T>(), anyArray, value)

/**
 * Remove the last element of array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#pop
 */
@VaultFunctionMarker
inline fun <reified T> PUSH(
    anyArray: AqlExpression<out List<T>>,
    value: AqlExpression<out T>,
    unique: AqlExpression<Boolean>,
): AqlExpression<List<T>> =
    AqlFunc.PUSH.arrayCall(type = kListType<T>(), anyArray, value, unique)

/**
 * Remove the element at position from the anyArray.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#remove_nth
 */
@VaultFunctionMarker
fun <T, N : Number> REMOVE_NTH(
    anyArray: AqlExpression<List<T>>,
    position: AqlExpression<N>,
): AqlExpression<List<T>> =
    AqlFunc.REMOVE_NTH.arrayCall(type = anyArray.getType(), anyArray, position)

/**
 * Remove the element at value from the anyArray.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#remove_value
 */
@VaultFunctionMarker
fun <T> REMOVE_VALUE(
    anyArray: AqlExpression<List<T>>,
    value: AqlExpression<T>,
): AqlExpression<List<T>> =
    AqlFunc.REMOVE_VALUE.arrayCall(type = anyArray.getType(), anyArray, value)

/**
 * Remove the element at value from the anyArray.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#remove_value
 */
@VaultFunctionMarker
fun <T, N : Number> REMOVE_VALUE(
    anyArray: AqlExpression<List<T>>,
    value: AqlExpression<T>,
    limit: AqlExpression<N>,
): AqlExpression<List<T>> =
    AqlFunc.REMOVE_VALUE.arrayCall(type = anyArray.getType(), anyArray, value, limit)

/**
 * Remove all occurrences of any of the values from anyArray.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#remove_values
 */
@VaultFunctionMarker
inline fun <reified T> REMOVE_VALUES(
    anyArray: AqlExpression<out List<T>>,
    values: AqlExpression<out List<T>>,
): AqlExpression<List<T>> =
    AqlFunc.REMOVE_VALUES.arrayCall(type = kListType<T>(), anyArray, values)

/**
 * Return an array with its elements reversed.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#reverse
 */
@VaultFunctionMarker
fun <T> REVERSE(
    anyArray: AqlExpression<List<T>>,
): AqlExpression<List<T>> =
    AqlFunc.REVERSE.arrayCall(anyArray.getType(), anyArray)

/**
 * Remove the first element of array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#shift
 */
@VaultFunctionMarker
fun <T> SHIFT(
    anyArray: AqlExpression<List<T>>,
): AqlExpression<List<T>> =
    AqlFunc.SHIFT.arrayCall(anyArray.getType(), anyArray)

/**
 * Extract a slice of anyArray.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#slice
 */
@VaultFunctionMarker
fun <T, N : Number> SLICE(
    anyArray: AqlExpression<List<T>>,
    start: AqlExpression<N>,
): AqlExpression<List<T>> =
    AqlFunc.SLICE.arrayCall(anyArray.getType(), anyArray, start)

/**
 * Extract a slice of anyArray.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#slice
 */
@VaultFunctionMarker
fun <T, NS : Number, NL : Number> SLICE(
    anyArray: AqlExpression<List<T>>,
    start: AqlExpression<NS>,
    length: AqlExpression<NL>,
): AqlExpression<List<T>> =
    AqlFunc.SLICE.arrayCall(anyArray.getType(), anyArray, start, length)

/**
 * Sort all elements in anyArray. The function will use the default comparison order for AQL value types.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#sorted
 */
@VaultFunctionMarker
fun <T> SORTED(
    anyArray: AqlExpression<List<T>>,
): AqlExpression<List<T>> =
    AqlFunc.SORTED.arrayCall(anyArray.getType(), anyArray)

/**
 * Sort all elements in anyArray. The function will use the default comparison order for AQL value types.
 * Additionally, the values in the result array will be made unique.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#sorted
 */
@VaultFunctionMarker
fun <T> SORTED_UNIQUE(
    anyArray: AqlExpression<List<T>>,
): AqlExpression<List<T>> =
    AqlFunc.SORTED_UNIQUE.arrayCall(anyArray.getType(), anyArray)

/**
 * Return the union of all arrays specified.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#union
 */
@VaultFunctionMarker
inline fun <reified T : Any> UNION(
    array1: AqlExpression<out List<T>>,
    array2: AqlExpression<out List<T>>,
    vararg arrayN: AqlExpression<out List<T>>,
): AqlExpression<List<T>> =
    AqlFunc.UNION.arrayCall(kListType<T>(), array1, array2, *arrayN)

/**
 * Return the union of distinct values of all arrays specified.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#union_destinct
 */
@VaultFunctionMarker
inline fun <reified T : Any> UNION_DISTINCT(
    array1: AqlExpression<out List<T>>,
    array2: AqlExpression<out List<T>>,
    vararg arrayN: AqlExpression<out List<T>>,
): AqlExpression<List<T>> =
    AqlFunc.UNION_DISTINCT.arrayCall(kListType<T>(), array1, array2, *arrayN)

/**
 * Return all unique elements in anyArray. To determine uniqueness, the function will use the comparison order.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#unique
 */
@VaultFunctionMarker
fun <T> UNIQUE(
    anyArray: AqlExpression<List<T>>,
): AqlExpression<List<T>> =
    AqlFunc.UNIQUE.arrayCall(anyArray.getType(), anyArray)

/**
 * Prepend value to anyArray (left side).
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#unshift
 */
@VaultFunctionMarker
inline fun <reified T> UNSHIFT(
    anyArray: AqlExpression<out List<T>>,
    value: AqlExpression<out T>,
): AqlExpression<List<T>> =
    AqlFunc.UNSHIFT.arrayCall(kListType<T>(), anyArray, value)

/**
 * Prepend value to anyArray (left side).
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#unshift
 */
@VaultFunctionMarker
inline fun <reified T> UNSHIFT(
    anyArray: AqlExpression<out List<T>>,
    value: AqlExpression<out T>,
    unique: AqlExpression<Boolean>,
): AqlExpression<List<T>> =
    AqlFunc.UNSHIFT.arrayCall(kListType<T>(), anyArray, value, unique)
