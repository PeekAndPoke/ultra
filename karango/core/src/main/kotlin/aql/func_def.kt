@file:Suppress("FunctionName")

package io.peekandpoke.karango.aql

import io.peekandpoke.ultra.reflection.kType

/**
 * Return the angle converted from radians to degrees.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#degrees
 */
val DEGREES = aqlFunc<Number>("DEGREES")


fun <T : Number> DEGREES(value: AqlExpression<T>): AqlExpression<Number> = DEGREES.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the encoded uri component of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#encodeuricomponent
 */
val ENCODE_URI_COMPONENT = aqlFunc<String>("ENCODE_URI_COMPONENT")

/** Return the encoded uri component of value. */

fun ENCODE_URI_COMPONENT(value: AqlExpression<String>): AqlExpression<String> =
    ENCODE_URI_COMPONENT.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return Euler's constant (2.71828...) raised to the power of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#exp
 */
val EXP = aqlFunc<Number>("EXP")

/** Return Euler's constant (2.71828...) raised to the power of value. */

fun <T : Number> EXP(value: AqlExpression<T>): AqlExpression<Number> = EXP.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return 2 raised to the power of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#exp2
 */
val EXP2 = aqlFunc<Number>("EXP2")

/** Return 2 raised to the power of value. */

fun <T : Number> EXP2(value: AqlExpression<T>): AqlExpression<Number> = EXP2.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Get the first element of an array. It is the same as anyArray[0].
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#first
 */
val FIRST = aqlFunc<Any?>("FIRST")

inline fun <reified T> FIRST(anyArray: AqlExpression<List<T>>): AqlExpression<T?> =
    FIRST.call(type = kType(), anyArray)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Turn an array of arrays into a flat array. All array elements in array will be expanded in the result array.
 * Non-array elements are added as they are. The function will recurse into sub-arrays up to the specified depth.
 * Duplicates will not be removed.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#flatten
 */
val FLATTEN = aqlFunc<List<Any?>>("FLATTEN")

/**
 * Turn an array of arrays into a flat array. All array elements in array will be expanded in the result array.
 */
inline fun <reified T> FLATTEN(anyArray: AqlExpression<List<T>>): AqlExpression<List<Any?>> =
    FLATTEN.call(anyArray)

/**
 * Turn an array of arrays into a flat array. All array elements in array will be expanded in the result array.
 */
inline fun <reified T, N : Number> FLATTEN(
    anyArray: AqlExpression<List<T>>, depth: AqlExpression<N>,
): AqlExpression<List<Any?>> =
    FLATTEN.call(anyArray, depth)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the position of the first occurrence of the string search inside the string text or -1 if not found.
 *
 * Positions start at 0.
 *
 * Returns position (number): the character position of the match. If search is not contained in text, -1 is returned.
 * If search is empty, start is returned.
 *
 * https://docs.arangodb.com/current/AQL/Functions/String.html#findfirst
 */
val FIND_FIRST = aqlFunc<Number>("FIND_FIRST")

/** Return the position of the first occurrence of the string search inside the string text or -1 if not found. */

fun FIND_FIRST(haystack: AqlExpression<String>, needle: AqlExpression<String>): AqlExpression<Number> =
    FIND_FIRST.call(haystack, needle)

/**
 * Return the position of the first occurrence of the string search inside the string text or -1 if not found.
 *
 * Starts search at the given [start] position.
 */

fun <T : Number> FIND_FIRST(
    haystack: AqlExpression<String>,
    needle: AqlExpression<String>,
    start: AqlExpression<T>,
): AqlExpression<Number> =
    FIND_FIRST.call(haystack, needle, start)

/**
 * Return the position of the first occurrence of the string search inside the string text or -1 if not found.
 *
 * Search between [start] and [end] positions (both inclusive).
 */

fun <T1 : Number, T2 : Number> FIND_FIRST(
    haystack: AqlExpression<String>,
    needle: AqlExpression<String>,
    start: AqlExpression<T1>,
    end: AqlExpression<T2>,
): AqlExpression<Number> =
    FIND_FIRST.call(haystack, needle, start, end)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the position of the last occurrence of the string search inside the string text or -1 if not found.
 *
 * Positions start at 0.
 *
 * Returns position (number): the character position of the match. If search is not contained in text, -1 is returned.
 * If search is empty, start is returned.
 *
 * https://docs.arangodb.com/current/AQL/Functions/String.html#findlast
 */
val FIND_LAST = aqlFunc<Number>("FIND_LAST")

/** Return the position of the last occurrence of the string search inside the string text or -1 if not found. */

fun FIND_LAST(haystack: AqlExpression<String>, needle: AqlExpression<String>): AqlExpression<Number> =
    FIND_LAST.call(haystack, needle)

/**
 * Return the position of the last occurrence of the string search inside the string text or -1 if not found.
 *
 * Starts search at the given [start] position.
 */

fun <T : Number> FIND_LAST(
    haystack: AqlExpression<String>,
    needle: AqlExpression<String>,
    start: AqlExpression<T>,
): AqlExpression<Number> =
    FIND_LAST.call(haystack, needle, start)

/**
 * Return the position of the last occurrence of the string search inside the string text or -1 if not found.
 *
 * Search between [start] and [end] positions (both inclusive).
 */

fun <T1 : Number, T2 : Number> FIND_LAST(
    haystack: AqlExpression<String>,
    needle: AqlExpression<String>,
    start: AqlExpression<T1>,
    end: AqlExpression<T2>,
): AqlExpression<Number> =
    FIND_LAST.call(haystack, needle, start, end)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the integer closest but not greater than value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#floor
 */
val FLOOR = aqlFunc<Number>("FLOOR")

/** Return the integer closest but not greater than value. */

fun <T : Number> FLOOR(value: AqlExpression<T>): AqlExpression<Number> = FLOOR.call(value)
