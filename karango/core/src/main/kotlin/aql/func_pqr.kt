@file:Suppress("FunctionName")

package io.peekandpoke.karango.aql

import io.peekandpoke.ultra.reflection.kListType
import io.peekandpoke.ultra.reflection.kType

/**
 * Return the nth percentile of the values in numArray.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#percentile
 */
val PERCENTILE = aqlFunc<Number>("PERCENTILE")

/**
 * Return the nth percentile of the values in numArray.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#percentile
 */

fun <T1 : Number, T2 : Number> PERCENTILE(
    numArray: AqlExpression<List<T1>>,
    n: AqlExpression<T2>,
): AqlExpression<Number> =
    PERCENTILE.call(numArray, n)

/** Return the nth percentile of the values in numArray. */

fun <T1 : Number?, T2 : Number> PERCENTILE(
    numArray: AqlExpression<List<T1>>,
    n: AqlExpression<T2>,
    method: AqlPercentileMethod,
): AqlExpression<Number> =
    PERCENTILE.call(numArray, n, method.method.aql)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Returns pi.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#pi
 */
val PI = aqlFunc<Number>("PI")

/** Returns pi. */

fun PI(): AqlExpression<Number> = PI.call()

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the base to the exponent exp.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#pow
 */
val POW = aqlFunc<Number>("POW")

/** Return the base to the exponent exp. */

fun <T1 : Number, T2 : Number> POW(base: AqlExpression<T1>, exp: AqlExpression<T2>): AqlExpression<Number> =
    POW.call(base, exp)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Remove the last element of array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#pop
 */
val POP = aqlFunc<List<Any?>>("POP")

/** Remove the last element of array. */

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
val POSITION = aqlFunc<Number>("POSITION")

/** Return whether [search] is contained in [anyArray]. */

fun <T> POSITION(anyArray: AqlExpression<List<T>>, search: AqlExpression<T>): AqlExpression<Number> =
    POSITION.call(anyArray, search, true.aql)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Append value to anyArray (right side).
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#push
 */
val PUSH = aqlFunc<List<Any?>>("PUSH")

/** Append value to anyArray (right side). */

inline fun <reified T> PUSH(anyArray: AqlExpression<out List<T>>, value: AqlExpression<out T>): AqlExpression<List<T>> =
    PUSH.call(type = kType(), anyArray, value)

/** Remove the last element of array. */

inline fun <reified T> PUSH(
    anyArray: AqlExpression<out List<T>>,
    value: AqlExpression<out T>,
    unique: AqlExpression<Boolean>,
): AqlExpression<List<T>> =
    PUSH.call(type = kType(), anyArray, value, unique)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the angle converted from degrees to radians.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#radians
 */
val RADIANS = aqlFunc<Number>("RADIANS")

/** Return the angle converted from degrees to radians. */

fun <T : Number> RADIANS(deg: AqlExpression<T>): AqlExpression<Number> = RADIANS.call(deg)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return a pseudo-random number between 0 and 1.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#rand
 */
val RAND = aqlFunc<Number>("RAND")

/** Return a pseudo-random number between 0 and 1. */

fun RAND(): AqlExpression<Number> = RAND.call()

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Generate a pseudo-random token string with the specified length.
 *
 * The algorithm for token generation should be treated as opaque.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#randomtoken
 */
val RANDOM_TOKEN = aqlFunc<String>("RANDOM_TOKEN")

/** Generate a pseudo-random token string with the specified length. */

fun <T : Number> RANDOM_TOKEN(length: AqlExpression<T>): AqlExpression<String> = RANDOM_TOKEN.call(length)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return an array of numbers in the specified range, optionally with increments other than 1.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#range
 */
val RANGE = aqlFunc<List<Number>?>("RANGE")

/** Return an array of numbers in the specified range, optionally with increments other than 1. */

fun <T1 : Number, T2 : Number> RANGE(
    start: AqlExpression<T1>,
    stop: AqlExpression<T2>,
): AqlExpression<List<Number>?> =
    RANGE.call(start, stop)

/** Return an array of numbers in the specified range, optionally with increments other than 1. */

fun <T1 : Number, T2 : Number, T3 : Number> RANGE(
    start: AqlExpression<T1>,
    stop: AqlExpression<T2>,
    step: AqlExpression<T3>,
): AqlExpression<List<Number>?> =
    RANGE.call(start, stop, step)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the matches in the given string text, using the regex.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#regexmatches
 */
val REGEX_MATCHES = aqlFunc<List<String>?>("REGEX_MATCHES")

/** Return the matches in the given string text, using the regex. */

fun REGEX_MATCHES(text: AqlExpression<String>, regex: AqlExpression<String>): AqlExpression<List<String>?> =
    REGEX_MATCHES.call(text, regex)

/** Return the matches in the given string text, using the regex. */

fun REGEX_MATCHES(
    text: AqlExpression<String>,
    regex: AqlExpression<String>,
    caseInsensitive: AqlExpression<Boolean>,
): AqlExpression<List<String>?> =
    REGEX_MATCHES.call(text, regex, caseInsensitive)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the matches in the given string text, using the regex.
 *
 * See https://docs.arangodb.com/stable/aql/functions/string/#regex_split
 */
val REGEX_SPLIT = aqlFunc<List<String>?>("REGEX_SPLIT")

/** Return the matches in the given string text, using the regex. */

fun REGEX_SPLIT(text: AqlExpression<String>, regex: AqlExpression<String>): AqlExpression<List<String>?> =
    REGEX_SPLIT.call(text, regex)

/** Return the matches in the given string text, using the regex. */

fun REGEX_SPLIT(
    text: AqlExpression<String>,
    regex: AqlExpression<String>,
    caseInsensitive: AqlExpression<Boolean>,
): AqlExpression<List<String>?> =
    REGEX_SPLIT.call(text, regex, caseInsensitive)

/** Return the matches in the given string text, using the regex. */

fun REGEX_SPLIT(
    text: AqlExpression<String>,
    regex: AqlExpression<String>,
    caseInsensitive: AqlExpression<Boolean>,
    limit: AqlExpression<Int>,
): AqlExpression<List<String>?> =
    REGEX_SPLIT.call(type = kListType<String>().nullable, text, regex, caseInsensitive, limit)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the matches in the given string text, using the regex.
 *
 * See https://docs.arangodb.com/stable/aql/functions/string/#regex_test
 */
val REGEX_TEST = aqlFunc<Boolean?>("REGEX_TEST")

/** Return the matches in the given string text, using the regex. */

fun REGEX_TEST(text: AqlExpression<String>, regex: AqlExpression<String>): AqlExpression<Boolean?> =
    REGEX_TEST.call(text, regex)

/** Return the matches in the given string text, using the regex. */

fun REGEX_TEST(
    text: AqlExpression<String>,
    regex: AqlExpression<String>,
    caseInsensitive: AqlExpression<Boolean>,
): AqlExpression<Boolean?> =
    REGEX_TEST.call(text, regex, caseInsensitive)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Remove the element at position from the anyArray.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#remove_nth
 */
val REMOVE_NTH = aqlFunc<List<Any?>>("REMOVE_NTH")

/** Remove the element at position from the anyArray. */

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
val REMOVE_VALUE = aqlFunc<List<Any?>>("REMOVE_VALUE")

/** Remove the element at value from the anyArray. */

inline fun <reified T> REMOVE_VALUE(anyArray: AqlExpression<List<T>>, value: AqlExpression<T>): AqlExpression<List<T>> =
    REMOVE_VALUE.call(type = kType(), anyArray, value)

/** Remove the element at value from the anyArray. */

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
val REMOVE_VALUES = aqlFunc<List<Any?>>("REMOVE_VALUES")

/** Remove all occurrences of any of the values from anyArray. */

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
val REVERSE = aqlFunc<Any>("REVERSE")

/** Return an array with its elements reversed. */
@JvmName("REVERSE_Array")
inline fun <reified T> REVERSE(anyArray: AqlExpression<List<T>>): AqlExpression<List<T>> =
    REVERSE.call(type = kType(), anyArray)

/** Return the reverse of the string value. */
@JvmName("REVERSE_String")
fun REVERSE_STRING(value: AqlExpression<String>): AqlExpression<String> =
    REVERSE.call(type = kType(), value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the n rightmost characters of the string value.
 *
 * https://docs.arangodb.com/current/AQL/Functions/String.html#right
 */
val RIGHT = aqlFunc<String>("RIGHT")

/** Return the n rightmost characters of the string value. */

fun <T : Number> RIGHT(value: AqlExpression<String>, n: AqlExpression<T>): AqlExpression<String> =
    RIGHT.call(value, n)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the integer closest to value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#round
 */
val ROUND = aqlFunc<Number>("ROUND")

/** Return the integer closest to value. */

fun <T : Number> ROUND(value: AqlExpression<T>): AqlExpression<Number> = ROUND.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the string value with whitespace stripped at the start only.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#rtrim
 */
val RTRIM = aqlFunc<String>("RTRIM")

/** Return the string value with whitespace stripped at the start only. */

fun RTRIM(subject: AqlExpression<String>): AqlExpression<String> = RTRIM.call(subject)

/** Return the string value with whitespace stripped at the start only. */

fun RTRIM(subject: AqlExpression<String>, chars: AqlExpression<String>): AqlExpression<String> =
    RTRIM.call(subject, chars)
