@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.lang.VaultFunctionMarker

/**
 * Merge the documents document1 to documentN into a single document.
 *
 * If document attribute keys are ambiguous, the merged result will contain the values of the documents
 * contained later in the argument list.
 *
 * See https://www.arangodb.com/docs/stable/aql/functions-document.html
 */
// TODO: write e3e tests
@VaultFunctionMarker val MERGE = aqlFunc<Any?>("MERGE")

/** Merge the documents document1 to documentN into a single document. */
@VaultFunctionMarker
inline fun <reified T> MERGE(
    document1: AqlExpression<out T>,
    document2: AqlExpression<out T>,
): AqlExpression<T> = MERGE.call(type = kType(), document1, document2)

/** Merge the documents document1 to documentN into a single document. */
@VaultFunctionMarker
inline fun <reified T> MERGE(
    document1: AqlExpression<out T>,
    document2: AqlExpression<out T>,
    vararg documentN: AqlExpression<out T>,
): AqlExpression<T> =
    MERGE.call(type = kType(), document1, document2, *documentN)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the difference of all arrays specified.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#minus
 */
@VaultFunctionMarker val MINUS = aqlFunc<List<Any?>>("MINUS")

/** Return the difference of all arrays specified. */
@VaultFunctionMarker
inline fun <reified T : Any> MINUS(
    array1: AqlExpression<out Collection<T>>,
    array2: AqlExpression<out Collection<T>>,
    vararg arrayN: AqlExpression<out Collection<T>>,
): AqlExpression<List<T>> =
    MINUS.call(type = kType(), array1, array2, *arrayN)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Get the element of an array at a given position.
 *
 * It is the same as anyArray(position) for positive positions, but does not support negative positions.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#nth
 */
@VaultFunctionMarker val NTH = aqlFunc<Any?>("NTH")

/** Get the element of an array at a given position. */
@VaultFunctionMarker
inline fun <reified T, N : Number> NTH(
    anyArray: AqlExpression<List<T>>,
    position: AqlExpression<N>,
): AqlExpression<T?> =
    NTH.call(type = kType(), anyArray, position)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the values that occur only once across all arrays specified.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#outersection
 */
@VaultFunctionMarker val OUTERSECTION = aqlFunc<List<Any?>>("OUTERSECTION")

/** Return the values that occur only once across all arrays specified. */
@VaultFunctionMarker
inline fun <reified T : Any> OUTERSECTION(
    array1: AqlExpression<out List<T>>,
    array2: AqlExpression<out List<T>>,
    vararg arrayN: AqlExpression<out List<T>>,
): AqlExpression<List<T>> =
    OUTERSECTION.call(type = kType(), array1, array2, *arrayN)
