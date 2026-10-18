@file:Suppress("FunctionName")

package io.peekandpoke.karango.aql

import io.peekandpoke.ultra.reflection.kType

/**
 * Return an AQL value described by the JSON-encoded input string.
 *
 * https://docs.arangodb.com/current/AQL/Functions/String.html#jsonparse
 */
val JSON_PARSE = aqlFunc<Any?>("JSON_PARSE")

/** Return an AQL value described by the JSON-encoded input string. */

fun JSON_PARSE(expr: AqlExpression<String>): AqlExpression<Any?> = JSON_PARSE.call(expr)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return a JSON string representation of the input value.
 *
 * https://docs.arangodb.com/current/AQL/Functions/String.html#jsonstringify
 */
val JSON_STRINGIFY = aqlFunc<String?>("JSON_STRINGIFY")

/** Return a JSON string representation of the input value. */

fun <T> JSON_STRINGIFY(expr: AqlExpression<T>): AqlExpression<String?> = JSON_STRINGIFY.call(expr)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Get the last element of an array. It is the same as anyArray[-1].
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#last
 */
val LAST = aqlFunc<Any?>("LAST")

/** Get the last element of an array. It is the same as anyArray[-1]. */

inline fun <reified T> LAST(anyArray: AqlExpression<List<T>>): AqlExpression<T?> =
    LAST.call(type = kType(), anyArray)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the n leftmost characters of the string value.
 *
 * https://docs.arangodb.com/current/AQL/Functions/String.html#left
 */
val LEFT = aqlFunc<String>("LEFT")

/** Return the n leftmost characters of the string value. */

fun <T : Number> LEFT(expr: AqlExpression<String>, n: AqlExpression<T>): AqlExpression<String> =
    LEFT.call(expr, n)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Determine the number of elements in an array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#length
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#length
 */
val LENGTH = aqlFunc<Number>("LENGTH")

/** Determine the number of elements in an array. */
@JvmName("LENGTH_Array")
fun <T> LENGTH(anyArray: AqlExpression<List<T>>): AqlExpression<Number> = LENGTH.call(anyArray)

/** Determine the character length of a string. */
@JvmName("LENGTH_String")
fun LENGTH(expr: AqlExpression<String>): AqlExpression<Number> = LENGTH.call(expr)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Calculate the Levenshtein distance between two strings.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#levenshteindistance
 */
val LEVENSHTEIN_DISTANCE = aqlFunc<Number>("LEVENSHTEIN_DISTANCE")

/** Calculate the Levenshtein distance between two strings. */

fun LEVENSHTEIN_DISTANCE(left: AqlExpression<String>, right: AqlExpression<String>): AqlExpression<Number> =
    LEVENSHTEIN_DISTANCE.call(left, right)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Check whether the pattern search is contained in the string text, using wildcard matching.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#like
 */
val LIKE = aqlFunc<Boolean>("LIKE")

/**
 * Check whether the pattern search is contained in the string text, using wildcard matching.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#like
 */

fun LIKE(text: AqlExpression<String>, search: AqlExpression<String>): AqlExpression<Boolean> =
    LIKE.call(text, search)

/**
 * Check whether the pattern search is contained in the string text, using wildcard matching.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#like
 */

fun LIKE(
    text: AqlExpression<String>,
    search: AqlExpression<String>,
    caseInsensitive: AqlExpression<Boolean>,
): AqlExpression<Boolean> =
    LIKE.call(text, search, caseInsensitive)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the natural logarithm of value. The base is Euler's constant (2.71828...).
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#log
 */
val LOG = aqlFunc<Number?>("LOG")

/** Return the natural logarithm of value. The base is Euler's constant (2.71828...). */

fun <T : Number> LOG(value: AqlExpression<T>): AqlExpression<Number?> = LOG.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the base 2 logarithm of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#log
 */
val LOG2 = aqlFunc<Number?>("LOG2")

/** Return the base 2 logarithm of value. */

fun <T : Number> LOG2(value: AqlExpression<T>): AqlExpression<Number?> = LOG2.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the base 10 logarithm of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#log
 */
val LOG10 = aqlFunc<Number?>("LOG10")

/** Return the base 10 logarithm of value. */

fun <T : Number> LOG10(value: AqlExpression<T>): AqlExpression<Number?> = LOG10.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Convert upper-case letters in value to their lower-case counterparts. All other characters are returned unchanged.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#lower
 */
val LOWER = aqlFunc<String>("LOWER")

/**
 *  Convert upper-case letters in value to their lower-case counterparts. All other characters are returned unchanged.
 */

fun LOWER(expr: AqlExpression<String>): AqlExpression<String> = LOWER.call(expr)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the string value with whitespace stripped from the start only.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#ltrim
 */
val LTRIM = aqlFunc<String>("LTRIM")

/** Return the string value with whitespace stripped from the start only. */

fun LTRIM(subject: AqlExpression<String>): AqlExpression<String> = LTRIM.call(subject)

/**
 * Return the string value with whitespace stripped from the start only.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#ltrim
 */

fun LTRIM(subject: AqlExpression<String>, chars: AqlExpression<String>): AqlExpression<String> =
    LTRIM.call(subject, chars)
