@file:Suppress("FunctionName")

package io.peekandpoke.karango.aql

import io.peekandpoke.ultra.reflection.kListType
import io.peekandpoke.ultra.reflection.kType

/**
 * Calculate the SHA1 checksum for text and returns it in a hexadecimal string representation.
 *
 * https://docs.arangodb.com/current/AQL/Functions/String.html#sha1
 */
val SHA1 = aqlFunc<String>("SHA1")

/** Calculate the SHA1 checksum for text and returns it in a hexadecimal string representation.*/
fun SHA1(expr: AqlExpression<String>): AqlExpression<String> = SHA1.call(expr)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Calculate the SHA512 checksum for text and returns it in a hexadecimal string representation.
 *
 * https://docs.arangodb.com/current/AQL/Functions/String.html#sha256
 */
val SHA512 = aqlFunc<String>("SHA512")

/** Calculate the SHA512 checksum for text and returns it in a hexadecimal string representation. */
fun SHA512(expr: AqlExpression<String>): AqlExpression<String> = SHA512.call(expr)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Remove the first element of array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#shift
 */
val SHIFT = aqlFunc<List<Any?>>("SHIFT")

inline fun <reified T> SHIFT(anyArray: AqlExpression<List<T>>): AqlExpression<List<T>> =
    SHIFT.call(type = kType(), anyArray)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the sine of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#sin
 */
val SIN = aqlFunc<Number>("SIN")

/** Return the sine of value. */
fun <T : Number> SIN(value: AqlExpression<T>): AqlExpression<Number> = SIN.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Extract a slice of anyArray.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#slice
 */
val SLICE = aqlFunc<List<Any?>>("SLICE")

/** Extract a slice of anyArray. */
inline fun <reified T, N : Number> SLICE(
    anyArray: AqlExpression<List<T>>,
    start: AqlExpression<N>,
): AqlExpression<List<T>> =
    SLICE.call(type = kType(), anyArray, start)

/** Extract a slice of anyArray. */
inline fun <reified T, NS : Number, NL : Number> SLICE(
    anyArray: AqlExpression<List<T>>,
    start: AqlExpression<NS>,
    length: AqlExpression<NL>,
): AqlExpression<List<T>> =
    SLICE.call(type = kType(), anyArray, start, length)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Sort all elements in anyArray. The function will use the default comparison order for AQL value types.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#sorted
 */
val SORTED = aqlFunc<List<Any?>>("SORTED")

/** Sort all elements in anyArray. The function will use the default comparison order for AQL value types. */
inline fun <reified T> SORTED(anyArray: AqlExpression<List<T>>): AqlExpression<List<T>> =
    SORTED.call(type = kType(), anyArray)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Split the given string value into a list of strings, using the separator.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#split
 */
val SPLIT = aqlFunc<List<String>>("SPLIT")

/** Split the given string value into a list of strings, using the separator. */
fun SPLIT(value: AqlExpression<String>, separator: AqlExpression<String>): AqlExpression<List<String>> =
    SPLIT.call(value, separator)

/** Split the given string value into a list of strings, using the separator. */
@JvmName("SPLIT2")
fun SPLIT(value: AqlExpression<String>, separators: AqlExpression<List<String>>): AqlExpression<List<String>> =
    SPLIT.call(value, separators)

/** Split the given string value into a list of strings, using the separator. */
fun <T : Number> SPLIT(
    value: AqlExpression<String>,
    separator: AqlExpression<String>,
    limit: AqlExpression<T>,
): AqlExpression<List<String>> =
    SPLIT.call(value, separator, limit)

/** Split the given string value into a list of strings, using the separator. */
@JvmName("SPLIT2")
fun <T : Number> SPLIT(
    value: AqlExpression<String>,
    separator: AqlExpression<List<String>>,
    limit: AqlExpression<T>,
): AqlExpression<List<String>> =
    SPLIT.call(type = kListType<String>(), value, separator, limit)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the square root of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#sqrt
 */
val SQRT = aqlFunc<Number?>("SQRT")

/** Return the square root of value. */
fun <T : Number> SQRT(value: AqlExpression<T>): AqlExpression<Number?> = SQRT.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Check whether the given string starts with prefix
 *
 * See https://www.arangodb.com/docs/stable/aql/functions-string.html#starts_with
 */
val STARTS_WITH = aqlFunc<Boolean>("STARTS_WITH")

fun STARTS_WITH(text: AqlExpression<String>, prefix: AqlExpression<String>): AqlExpression<Boolean> =
    STARTS_WITH.call(text, prefix)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the population standard deviation of the values in array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#stddevpopulation
 */
val STDDEV_POPULATION = aqlFunc<Number?>("STDDEV_POPULATION")

/** Return the population standard deviation of the values in array. */
fun <T : Number> STDDEV_POPULATION(value: AqlExpression<List<T>>): AqlExpression<Number?> =
    STDDEV_POPULATION.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the sample standard deviation of the values in array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#stddevsample
 */
val STDDEV_SAMPLE = aqlFunc<Number?>("STDDEV_SAMPLE")

/** Return the sample standard deviation of the values in array. */
fun <T : Number> STDDEV_SAMPLE(value: AqlExpression<List<T>>): AqlExpression<Number?> = STDDEV_SAMPLE.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the population standard deviation of the values in array.
 *
 * Alias of STDDEV_POPULATION
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#stddev
 */
val STDDEV = aqlFunc<Number>("STDDEV")

/** Return the population standard deviation of the values in array. */
fun <T : Number> STDDEV(value: AqlExpression<List<T>>): AqlExpression<Number> = STDDEV.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the soundex fingerprint of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#soundex
 */
val SOUNDEX = aqlFunc<String>("SOUNDEX")

/** Return the soundex fingerprint of value. */
fun SOUNDEX(value: AqlExpression<String>): AqlExpression<String> = SOUNDEX.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return a substring of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#substring
 */
val SUBSTRING = aqlFunc<String>("SUBSTRING")

/** Return a substring of value. */
fun <T : Number> SUBSTRING(value: AqlExpression<String>, offset: AqlExpression<T>): AqlExpression<String> =
    SUBSTRING.call(value, offset)

/** Return a substring of value. */
fun <T1 : Number, T2 : Number> SUBSTRING(
    value: AqlExpression<String>,
    offset: AqlExpression<T1>,
    length: AqlExpression<T2>,
): AqlExpression<String> =
    SUBSTRING.call(value, offset, length)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the sum of the values in array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#stddevsample
 */
val SUM = aqlFunc<Number>("SUM")

/** Return the sum of the values in array. */
fun <T : Number> SUM(numArray: AqlExpression<List<T>>): AqlExpression<Number> = SUM.call(numArray)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Sort all elements in anyArray and remove duplicates.
 *
 * The function will use the default comparison order for AQL value types.
 * Additionally, the values in the result array will be made unique.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#sorted
 */
val SORTED_UNIQUE = aqlFunc<List<Any?>>("SORTED_UNIQUE")

/** Sort all elements in anyArray and remove duplicates. */
inline fun <reified T> SORTED_UNIQUE(anyArray: AqlExpression<List<T>>): AqlExpression<List<T>> =
    SORTED_UNIQUE.call(type = kType(), anyArray)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the tangent of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#tan
 */
val TAN = aqlFunc<Number>("TAN")

/** Return the tangent of value. */
fun <T : Number> TAN(value: AqlExpression<T>): AqlExpression<Number> = TAN.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the base64 representation of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#tobase64
 */
val TO_BASE64 = aqlFunc<String>("TO_BASE64")

/** Return the base64 representation of value. */
fun TO_BASE64(value: AqlExpression<String>): AqlExpression<String> = TO_BASE64.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the hex representation of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#tobase64
 */
val TO_HEX = aqlFunc<String>("TO_HEX")

/** Return the hex representation of value. */
fun TO_HEX(value: AqlExpression<String>): AqlExpression<String> = TO_HEX.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the string value with whitespace stripped from start and end
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#trim
 */
val TRIM = aqlFunc<String>("TRIM")

/** Return the string value with whitespace stripped from start and end */
fun TRIM(subject: AqlExpression<String>): AqlExpression<String> = TRIM.call(subject)

/** Return the string value with whitespace stripped from start and end */
fun TRIM(subject: AqlExpression<String>, chars: AqlExpression<String>): AqlExpression<String> =
    TRIM.call(subject, chars)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the union of all arrays specified.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#union
 */
val UNION = aqlFunc<List<Any?>>("UNION")

/** Return the union of all arrays specified. */
inline fun <reified T : Any> UNION(
    array1: AqlExpression<out List<T>>,
    array2: AqlExpression<out List<T>>,
    vararg arrayN: AqlExpression<out List<T>>,
): AqlExpression<List<T>> =
    UNION.call(type = kType(), array1, array2, *arrayN)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the union of distinct values of all arrays specified.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#union_destinct
 */
val UNION_DISTINCT = aqlFunc<List<Any?>>("UNION_DISTINCT")

/** Return the union of distinct values of all arrays specified. */
inline fun <reified T : Any> UNION_DISTINCT(
    array1: AqlExpression<out List<T>>,
    array2: AqlExpression<out List<T>>,
    vararg arrayN: AqlExpression<out List<T>>,
): AqlExpression<List<T>> =
    UNION_DISTINCT.call(type = kType(), array1, array2, *arrayN)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return all unique elements in anyArray. To determine uniqueness, the function will use the comparison order.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#unique
 */
val UNIQUE = aqlFunc<List<Any?>>("UNIQUE")

/** Return all unique elements in anyArray. To determine uniqueness, the function will use the comparison order. */
inline fun <reified T> UNIQUE(anyArray: AqlExpression<List<T>>): AqlExpression<List<T>> =
    UNIQUE.call(type = kType(), anyArray)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
val UNSET = aqlFunc<Any>("UNSET")

inline fun <reified T> UNSET(
    document: AqlExpression<out T>,
    attributeName1: AqlExpression<String>,
    vararg attributeNameN: AqlExpression<String>,
): AqlExpression<T> =
    UNSET.call(type = kType(), document, attributeName1, *attributeNameN)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Prepend value to anyArray (left side).
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#unshift
 */
val UNSHIFT = aqlFunc<List<Any?>>("UNSHIFT")

/** Prepend value to anyArray (left side). */
inline fun <reified T> UNSHIFT(
    anyArray: AqlExpression<out List<T>>,
    value: AqlExpression<out T>,
): AqlExpression<List<T>> =
    UNSHIFT.call(type = kType(), anyArray, value)

/** Prepend value to anyArray (left side). */
inline fun <reified T> UNSHIFT(
    anyArray: AqlExpression<out List<T>>,
    value: AqlExpression<out T>,
    unique: AqlExpression<Boolean>,
): AqlExpression<List<T>> =
    UNSHIFT.call(type = kType(), anyArray, value, unique)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Convert lower-case letters in value to their upper-case counterparts. All other characters are returned unchanged.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#upper
 */
val UPPER = aqlFunc<String>("UPPER")

/**
 * Convert lower-case letters in value to their upper-case counterparts. All other characters are returned unchanged.
 */
fun UPPER(expr: AqlExpression<String>): AqlExpression<String> = UPPER.call(expr)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return a universally unique identifier value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#uuid
 */
val UUID = aqlFunc<String>("UUID")

/** Return a universally unique identifier value. */
fun UUID(): AqlExpression<String> = UUID.call()
