@file:Suppress("FunctionName")

package io.peekandpoke.karango.aql

import io.peekandpoke.ultra.reflection.kType

/**
 * Return the intersection of all arrays specified. The result is an array of values that occur in all arguments.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#intersection
 */
val INTERSECTION = aqlFunc<List<Any?>>("INTERSECTION")

/**
 * Return the intersection of all arrays specified. The result is an array of values that occur in all arguments.
 */
inline fun <reified T : Any> INTERSECTION(
    array1: AqlExpression<out Collection<T>>,
    array2: AqlExpression<out Collection<T>>,
    vararg arrayN: AqlExpression<out Collection<T>>,
): AqlExpression<List<T>> =
    INTERSECTION.call(type = kType(), array1, array2, *arrayN)
