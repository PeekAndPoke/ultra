@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.lang.VaultFunctionMarker

/**
 * Get the first element of an array. It is the same as anyArray[0].
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#first
 */
@VaultFunctionMarker val FIRST = aqlFunc<Any?>("FIRST")

@VaultFunctionMarker inline fun <reified T> FIRST(anyArray: AqlExpression<List<T>>): AqlExpression<T?> =
    FIRST.call(type = kType(), anyArray)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Turn an array of arrays into a flat array. All array elements in array will be expanded in the result array.
 * Non-array elements are added as they are. The function will recurse into sub-arrays up to the specified depth.
 * Duplicates will not be removed.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#flatten
 */
@VaultFunctionMarker val FLATTEN = aqlFunc<List<Any?>>("FLATTEN")

/**
 * Turn an array of arrays into a flat array. All array elements in array will be expanded in the result array.
 */
@VaultFunctionMarker inline fun <reified T> FLATTEN(anyArray: AqlExpression<List<T>>): AqlExpression<List<Any?>> =
    FLATTEN.call(anyArray)

/**
 * Turn an array of arrays into a flat array. All array elements in array will be expanded in the result array.
 * Non-array elements are added as they are. The function will recurse into sub-arrays up to the specified depth.
 * Duplicates will not be removed.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#flatten
 */
@VaultFunctionMarker inline fun <reified T, N : Number> FLATTEN(
    anyArray: AqlExpression<List<T>>, depth: AqlExpression<N>,
): AqlExpression<List<Any?>> =
    FLATTEN.call(anyArray, depth)
