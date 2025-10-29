@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.lang.VaultFunctionMarker

/**
 * Return the angle converted from radians to degrees.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#degrees
 */
@VaultFunctionMarker val DEGREES = aqlFunc<Number>("DEGREES")

@VaultFunctionMarker
fun <T : Number> DEGREES(value: AqlExpression<T>): AqlExpression<Number> = DEGREES.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the encoded uri component of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#encodeuricomponent
 */
@VaultFunctionMarker val ENCODE_URI_COMPONENT = aqlFunc<String>("ENCODE_URI_COMPONENT")

/** Return the encoded uri component of value. */
@VaultFunctionMarker
fun ENCODE_URI_COMPONENT(value: AqlExpression<String>): AqlExpression<String> =
    ENCODE_URI_COMPONENT.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return Euler's constant (2.71828...) raised to the power of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#exp
 */
@VaultFunctionMarker val EXP = aqlFunc<Number>("EXP")

/** Return Euler's constant (2.71828...) raised to the power of value. */
@VaultFunctionMarker
fun <T : Number> EXP(value: AqlExpression<T>): AqlExpression<Number> = EXP.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return 2 raised to the power of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#exp2
 */
@VaultFunctionMarker val EXP2 = aqlFunc<Number>("EXP2")

/** Return 2 raised to the power of value. */
@VaultFunctionMarker
fun <T : Number> EXP2(value: AqlExpression<T>): AqlExpression<Number> = EXP2.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
 */
@VaultFunctionMarker inline fun <reified T, N : Number> FLATTEN(
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
@VaultFunctionMarker val FIND_FIRST = aqlFunc<Number>("FIND_FIRST")

/** Return the position of the first occurrence of the string search inside the string text or -1 if not found. */
@VaultFunctionMarker
fun FIND_FIRST(haystack: AqlExpression<String>, needle: AqlExpression<String>): AqlExpression<Number> =
    FIND_FIRST.call(haystack, needle)

/**
 * Return the position of the first occurrence of the string search inside the string text or -1 if not found.
 *
 * Starts search at the given [start] position.
 */
@VaultFunctionMarker
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
@VaultFunctionMarker
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
@VaultFunctionMarker val FIND_LAST = aqlFunc<Number>("FIND_LAST")

/** Return the position of the last occurrence of the string search inside the string text or -1 if not found. */
@VaultFunctionMarker
fun FIND_LAST(haystack: AqlExpression<String>, needle: AqlExpression<String>): AqlExpression<Number> =
    FIND_LAST.call(haystack, needle)

/**
 * Return the position of the last occurrence of the string search inside the string text or -1 if not found.
 *
 * Starts search at the given [start] position.
 */
@VaultFunctionMarker
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
@VaultFunctionMarker
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
@VaultFunctionMarker val FLOOR = aqlFunc<Number>("FLOOR")

/** Return the integer closest but not greater than value. */
@VaultFunctionMarker
fun <T : Number> FLOOR(value: AqlExpression<T>): AqlExpression<Number> = FLOOR.call(value)

