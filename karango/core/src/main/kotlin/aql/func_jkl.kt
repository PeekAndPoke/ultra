@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.lang.VaultFunctionMarker

/**
 * Return an AQL value described by the JSON-encoded input string.
 *
 * https://docs.arangodb.com/current/AQL/Functions/String.html#jsonparse
 */
@VaultFunctionMarker val JSON_PARSE = aqlFunc<Any?>("JSON_PARSE")

/** Return an AQL value described by the JSON-encoded input string. */
@VaultFunctionMarker
fun JSON_PARSE(expr: AqlExpression<String>): AqlExpression<Any?> = JSON_PARSE.call(expr)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return a JSON string representation of the input value.
 *
 * https://docs.arangodb.com/current/AQL/Functions/String.html#jsonstringify
 */
@VaultFunctionMarker val JSON_STRINGIFY = aqlFunc<String?>("JSON_STRINGIFY")

/** Return a JSON string representation of the input value. */
@VaultFunctionMarker
fun <T> JSON_STRINGIFY(expr: AqlExpression<T>): AqlExpression<String?> = JSON_STRINGIFY.call(expr)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Get the last element of an array. It is the same as anyArray[-1].
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#last
 */
@VaultFunctionMarker val LAST = aqlFunc<Any?>("LAST")

/** Get the last element of an array. It is the same as anyArray[-1]. */
@VaultFunctionMarker
inline fun <reified T> LAST(anyArray: AqlExpression<List<T>>): AqlExpression<T?> =
    LAST.call(type = kType(), anyArray)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the n leftmost characters of the string value.
 *
 * https://docs.arangodb.com/current/AQL/Functions/String.html#left
 */
@VaultFunctionMarker val LEFT = aqlFunc<String>("LEFT")

/** Return the n leftmost characters of the string value. */
@VaultFunctionMarker
fun <T : Number> LEFT(expr: AqlExpression<String>, n: AqlExpression<T>): AqlExpression<String> =
    LEFT.call(expr, n)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Determine the number of elements in an array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#length
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#length
 */
@VaultFunctionMarker val LENGTH = aqlFunc<Number>("LENGTH")

/** Determine the number of elements in an array. */
@VaultFunctionMarker @JvmName("LENGTH_Array")
fun <T> LENGTH(anyArray: AqlExpression<List<T>>): AqlExpression<Number> = LENGTH.call(anyArray)

/** Determine the character length of a string. */
@VaultFunctionMarker @JvmName("LENGTH_String")
fun LENGTH(expr: AqlExpression<String>): AqlExpression<Number> = LENGTH.call(expr)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Calculate the Levenshtein distance between two strings.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#levenshteindistance
 */
@VaultFunctionMarker val LEVENSHTEIN_DISTANCE = aqlFunc<Number>("LEVENSHTEIN_DISTANCE")

/** Calculate the Levenshtein distance between two strings. */
@VaultFunctionMarker
fun LEVENSHTEIN_DISTANCE(left: AqlExpression<String>, right: AqlExpression<String>): AqlExpression<Number> =
    LEVENSHTEIN_DISTANCE.call(left, right)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Check whether the pattern search is contained in the string text, using wildcard matching.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#like
 */
@VaultFunctionMarker val LIKE = aqlFunc<Boolean>("LIKE")

/**
 * Check whether the pattern search is contained in the string text, using wildcard matching.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#like
 */
@VaultFunctionMarker
fun LIKE(text: AqlExpression<String>, search: AqlExpression<String>): AqlExpression<Boolean> =
    LIKE.call(text, search)

/**
 * Check whether the pattern search is contained in the string text, using wildcard matching.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#like
 */
@VaultFunctionMarker
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
@VaultFunctionMarker val LOG = aqlFunc<Number?>("LOG")

/** Return the natural logarithm of value. The base is Euler's constant (2.71828...). */
@VaultFunctionMarker
fun <T : Number> LOG(value: AqlExpression<T>): AqlExpression<Number?> = LOG.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the base 2 logarithm of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#log
 */
@VaultFunctionMarker val LOG2 = aqlFunc<Number?>("LOG2")

/** Return the base 2 logarithm of value. */
@VaultFunctionMarker
fun <T : Number> LOG2(value: AqlExpression<T>): AqlExpression<Number?> = LOG2.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the base 10 logarithm of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#log
 */
@VaultFunctionMarker val LOG10 = aqlFunc<Number?>("LOG10")

/** Return the base 10 logarithm of value. */
@VaultFunctionMarker
fun <T : Number> LOG10(value: AqlExpression<T>): AqlExpression<Number?> = LOG10.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Convert upper-case letters in value to their lower-case counterparts. All other characters are returned unchanged.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#lower
 */
@VaultFunctionMarker val LOWER = aqlFunc<String>("LOWER")

/**
 *  Convert upper-case letters in value to their lower-case counterparts. All other characters are returned unchanged.
 */
@VaultFunctionMarker
fun LOWER(expr: AqlExpression<String>): AqlExpression<String> = LOWER.call(expr)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the string value with whitespace stripped from the start only.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#ltrim
 */
@VaultFunctionMarker val LTRIM = aqlFunc<String>("LTRIM")

/** Return the string value with whitespace stripped from the start only. */
@VaultFunctionMarker
fun LTRIM(subject: AqlExpression<String>): AqlExpression<String> = LTRIM.call(subject)

/**
 * Return the string value with whitespace stripped from the start only.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#ltrim
 */
@VaultFunctionMarker
fun LTRIM(subject: AqlExpression<String>, chars: AqlExpression<String>): AqlExpression<String> =
    LTRIM.call(subject, chars)
