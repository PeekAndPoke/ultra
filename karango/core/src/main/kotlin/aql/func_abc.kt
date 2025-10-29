@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.lang.VaultFunctionMarker

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the absolute part of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#abs
 */
@VaultFunctionMarker val ABS = aqlFunc<Number>("ABS")

/** Return the absolute part of value. */
@VaultFunctionMarker
inline fun <reified T : Number> ABS(value: AqlExpression<T>): AqlExpression<T> = ABS.call(type = kType(), value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the arccosine of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#acos
 */
@VaultFunctionMarker val ACOS = aqlFunc<Number?>("ACOS")

/** Return the arccosine of value. */
@VaultFunctionMarker
fun <T : Number> ACOS(value: AqlExpression<T>): AqlExpression<Number?> = ACOS.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Add all elements of an array to another array. All values are added at the end of the array (right side).
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#append
 */
@VaultFunctionMarker val APPEND = aqlFunc<List<Any>>("APPEND")

/** Add all elements of an array to another array. All values are added at the end of the array (right side). */
@VaultFunctionMarker
inline fun <reified T> APPEND(
    anyArray: AqlExpression<out List<T>>, values: AqlExpression<out List<T>>,
): AqlExpression<List<T>> =
    APPEND.call(type = kType(), anyArray, values)

/** Add all elements of an array to another array. All values are added at the end of the array (right side). */
@VaultFunctionMarker
inline fun <reified T> APPEND(
    anyArray: AqlExpression<out List<T>>, values: AqlExpression<out List<T>>, unique: AqlExpression<Boolean>,
): AqlExpression<List<T>> =
    APPEND.call(type = kType(), anyArray, values, unique)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the arcsine of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#asin
 */
@VaultFunctionMarker val ASIN = aqlFunc<Number?>("ASIN")

/** Return the arcsine of value. */
@VaultFunctionMarker
fun <T : Number> ASIN(value: AqlExpression<T>): AqlExpression<Number?> = ASIN.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the arctangent of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#atan
 */
@VaultFunctionMarker val ATAN = aqlFunc<Number>("ATAN")

/** Return the arctangent of value. */
@VaultFunctionMarker
fun <T : Number> ATAN(value: AqlExpression<T>): AqlExpression<Number> = ATAN.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the arctangent of the quotient of y and x.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#atan2
 */
@VaultFunctionMarker val ATAN2 = aqlFunc<Number>("ATAN2")

/** Return the arctangent of the quotient of y and x. */
@VaultFunctionMarker
fun <T1 : Number, T2 : Number> ATAN2(x: AqlExpression<T1>, y: AqlExpression<T2>): AqlExpression<Number> =
    ATAN2.call(x, y)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the average (arithmetic mean) of the values in array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#average
 */
@VaultFunctionMarker val AVERAGE = aqlFunc<Number?>("AVERAGE")

/** Return the average (arithmetic mean) of the values in array. */
@VaultFunctionMarker
fun <T : Number> AVERAGE(numArray: AqlExpression<out Collection<T>>): AqlExpression<Number?> =
    AVERAGE.call(numArray)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the average (arithmetic mean) of the values in array.
 *
 * Alias of AVERAGE
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#avg
 */
@VaultFunctionMarker val AVG = aqlFunc<Number?>("AVG")

/** Return the average (arithmetic mean) of the values in array. */
@VaultFunctionMarker
fun <T : Number> AVG(numArray: AqlExpression<out Collection<T>>): AqlExpression<Number?> =
    AVG.call(numArray)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the integer closest but not less than value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#ceil
 */
@VaultFunctionMarker val CEIL = aqlFunc<Number>("CEIL")

/** Return the integer closest but not less than value. */
@VaultFunctionMarker
fun <T : Number> CEIL(value: AqlExpression<T>): AqlExpression<Number> = CEIL.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the cosine of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#cos
 */
@VaultFunctionMarker val COS = aqlFunc<Number>("COS")

/** Return the cosine of value. */
@VaultFunctionMarker
fun <T : Number> COS(value: AqlExpression<T>): AqlExpression<Number> = COS.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the number of characters in value (not byte length).
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#charlength
 */
@VaultFunctionMarker val CHAR_LENGTH = aqlFunc<Number>("CHAR_LENGTH")

/** Return the number of characters in value (not byte length). */
@VaultFunctionMarker
fun CHAR_LENGTH(expr: AqlExpression<String>): AqlExpression<Number> = CHAR_LENGTH.call(expr)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Concatenate the values passed as value1 to valueN.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#concat
 */
@VaultFunctionMarker val CONCAT = aqlFunc<String>("CONCAT")

/** Concatenate the values passed as value1 to valueN. */
@VaultFunctionMarker
fun CONCAT(first: AqlExpression<String>, vararg rest: AqlExpression<String>): AqlExpression<String> =
    CONCAT.call(first, *rest)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Concatenate the strings passed as arguments value1 to valueN using the separator string.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#concatseparator
 */
@VaultFunctionMarker val CONCAT_SEPARATOR = aqlFunc<String>("CONCAT_SEPARATOR")

/** Concatenate the strings passed as arguments value1 to valueN using the separator string. */
@VaultFunctionMarker
fun CONCAT_SEPARATOR(
    separator: AqlExpression<String>,
    first: AqlExpression<String>,
    vararg rest: AqlExpression<String>,
): AqlExpression<String> =
    CONCAT_SEPARATOR.call(separator, first, *rest)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Check whether the string search is contained in the string text.
 *
 * The string matching performed by CONTAINS is case-sensitive.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#contains
 */
@VaultFunctionMarker val CONTAINS = aqlFunc<Boolean>("CONTAINS")

/** Check whether the string search is contained in the string text. */
@VaultFunctionMarker
fun CONTAINS(haystack: AqlExpression<String>, needle: AqlExpression<String>): AqlExpression<Boolean> =
    CONTAINS.call(haystack, needle)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return whether search is contained in anyArray.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#contains_array
 *
 * To get the position of the occurrence, use CONTAINS_ARRAY_IDX()
 */
@VaultFunctionMarker val CONTAINS_ARRAY = aqlFunc<Boolean>("CONTAINS_ARRAY")

/** Return whether [search] is contained in [anyArray]. */
@VaultFunctionMarker
fun <T> CONTAINS_ARRAY(
    anyArray: AqlExpression<out Collection<T>>, search: AqlExpression<T>,
): AqlExpression<Boolean> =
    CONTAINS_ARRAY.call(anyArray, search)

/** Return the position of the match (starting with 0) otherwise -1. */
@VaultFunctionMarker fun <T> CONTAINS_ARRAY_IDX(
    anyArray: AqlExpression<List<T>>, search: AqlExpression<T>,
): AqlExpression<Number> =
    CONTAINS_ARRAY.call(type = kType(), anyArray, search, true.aql)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Determine the character length of a string.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#count
 * see https://docs.arangodb.com/current/AQL/Functions/Array.html#count
 */
@VaultFunctionMarker val COUNT = aqlFunc<Int>("COUNT")

/** Determine the character length of a string. */
@VaultFunctionMarker @JvmName("COUNT_String")
fun COUNT(expr: AqlExpression<String>): AqlExpression<Int> = COUNT.call(expr)

/** Determine the number of elements in an array. */
@VaultFunctionMarker @JvmName("COUNT_Array")
fun <T> COUNT(expr: AqlExpression<out Collection<T>>): AqlExpression<Int> = COUNT.call(expr)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Determine the number of distinct elements in an array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#count
 */
@VaultFunctionMarker val COUNT_DISTINCT = aqlFunc<Int>("COUNT_DISTINCT")

/** Determine the number of distinct elements in an array. */
@VaultFunctionMarker
fun <T> COUNT_DISTINCT(anyArray: AqlExpression<out Collection<T>>): AqlExpression<Int> = COUNT_DISTINCT.call(anyArray)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Determine the number of distinct elements in an array.
 *
 * Alias of COUNT_DISTINCT
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Array.html#count
 */
@VaultFunctionMarker val COUNT_UNIQUE = aqlFunc<Int>("COUNT_UNIQUE")

@VaultFunctionMarker
fun <T> COUNT_UNIQUE(anyArray: AqlExpression<out Collection<T>>): AqlExpression<Int> = COUNT_UNIQUE.call(anyArray)
