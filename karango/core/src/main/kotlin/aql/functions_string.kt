@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.kListType
import de.peekandpoke.ultra.vault.lang.VaultFunctionMarker

/**
 * Return the number of characters in value (not byte length).
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#charlength
 */
@VaultFunctionMarker
fun CHAR_LENGTH(
    expr: AqlExpression<String>,
): AqlExpression<Number> =
    AqlFunc.CHAR_LENGTH.numberCall(expr)

/**
 * Concatenate the values passed as value1 to valueN.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#concat
 */
@VaultFunctionMarker
fun CONCAT(
    first: AqlExpression<String>,
    vararg rest: AqlExpression<String>,
): AqlExpression<String> =
    AqlFunc.CONCAT.stringCall(first, *rest)

/**
 * Concatenate the strings passed as arguments value1 to valueN using the separator string.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#concatseparator
 */
@VaultFunctionMarker
fun CONCAT_SEPARATOR(
    separator: AqlExpression<String>,
    first: AqlExpression<String>,
    vararg rest: AqlExpression<String>,
): AqlExpression<String> =
    AqlFunc.CONCAT_SEPARATOR.stringCall(separator, first, *rest)

/**
 * Check whether the string search is contained in the string text. The string matching performed by CONTAINS is case-sensitive.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#contains
 */
@VaultFunctionMarker
fun CONTAINS(
    haystack: AqlExpression<String>,
    needle: AqlExpression<String>,
): AqlExpression<Boolean> =
    AqlFunc.CONTAINS.boolCall(haystack, needle)

/**
 * Return the encoded uri component of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#encodeuricomponent
 */
@VaultFunctionMarker
fun ENCODE_URI_COMPONENT(value: AqlExpression<String>): AqlExpression<String> =
    AqlFunc.ENCODE_URI_COMPONENT.stringCall(value)

/**
 * Return the position of the first occurrence of the string search inside the string text. Positions start at 0.
 *
 * https://docs.arangodb.com/current/AQL/Functions/String.html#findfirst
 */
@VaultFunctionMarker
fun FIND_FIRST(
    haystack: AqlExpression<String>,
    needle: AqlExpression<String>,
): AqlExpression<Number> =
    AqlFunc.FIND_FIRST.numberCall(haystack, needle)

/**
 * Return the position of the first occurrence of the string search inside the string text. Positions start at 0.
 *
 * https://docs.arangodb.com/current/AQL/Functions/String.html#findfirst
 */
@VaultFunctionMarker
fun <T : Number> FIND_FIRST(
    haystack: AqlExpression<String>,
    needle: AqlExpression<String>,
    start: AqlExpression<T>,
): AqlExpression<Number> =
    AqlFunc.FIND_FIRST.numberCall(haystack, needle, start)

/**
 * Return the position of the first occurrence of the string search inside the string text. Positions start at 0.
 *
 * https://docs.arangodb.com/current/AQL/Functions/String.html#findfirst
 */
@VaultFunctionMarker
fun <T1 : Number, T2 : Number> FIND_FIRST(
    haystack: AqlExpression<String>,
    needle: AqlExpression<String>,
    start: AqlExpression<T1>,
    end: AqlExpression<T2>,
): AqlExpression<Number> =
    AqlFunc.FIND_FIRST.numberCall(haystack, needle, start, end)

/**
 * Return the position of the first occurrence of the string search inside the string text. Positions start at 0.
 *
 * https://docs.arangodb.com/current/AQL/Functions/String.html#findlast
 */
@VaultFunctionMarker
fun FIND_LAST(
    haystack: AqlExpression<String>,
    needle: AqlExpression<String>,
): AqlExpression<Number> =
    AqlFunc.FIND_LAST.numberCall(haystack, needle)

/**
 * Return the position of the first occurrence of the string search inside the string text. Positions start at 0.
 *
 * https://docs.arangodb.com/current/AQL/Functions/String.html#findlast
 */
@VaultFunctionMarker
fun <T : Number> FIND_LAST(
    haystack: AqlExpression<String>,
    needle: AqlExpression<String>,
    start: AqlExpression<T>,
): AqlExpression<Number> =
    AqlFunc.FIND_LAST.numberCall(haystack, needle, start)

/**
 * Return the position of the first occurrence of the string search inside the string text. Positions start at 0.
 *
 * https://docs.arangodb.com/current/AQL/Functions/String.html#findlast
 */
@VaultFunctionMarker
fun <T1 : Number, T2 : Number> FIND_LAST(
    haystack: AqlExpression<String>,
    needle: AqlExpression<String>,
    start: AqlExpression<T1>,
    end: AqlExpression<T2>,
): AqlExpression<Number> =
    AqlFunc.FIND_LAST.numberCall(haystack, needle, start, end)

/**
 * Return an AQL value described by the JSON-encoded input string.
 *
 * https://docs.arangodb.com/current/AQL/Functions/String.html#jsonparse
 */
@VaultFunctionMarker
fun JSON_PARSE(
    expr: AqlExpression<String>,
): AqlExpression<Any?> =
    AqlFunc.JSON_PARSE.nullableAnyCall(expr)

/**
 * Return a JSON string representation of the input value.
 *
 * https://docs.arangodb.com/current/AQL/Functions/String.html#jsonstringify
 */
@VaultFunctionMarker
fun <T> JSON_STRINGIFY(
    expr: AqlExpression<T>,
): AqlExpression<String> =
    AqlFunc.JSON_STRINGIFY.stringCall(expr)

/**
 * Return the n leftmost characters of the string value.
 *
 * https://docs.arangodb.com/current/AQL/Functions/String.html#left
 */
@VaultFunctionMarker
fun <T : Number> LEFT(
    expr: AqlExpression<String>,
    n: AqlExpression<T>,
): AqlExpression<String> =
    AqlFunc.LEFT.stringCall(expr, n)


/**
 * Calculate the Levenshtein distance between two strings.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#levenshteindistance
 */
@VaultFunctionMarker
fun LEVENSHTEIN_DISTANCE(
    left: AqlExpression<String>,
    right: AqlExpression<String>,
): AqlExpression<Number> =
    AqlFunc.LEVENSHTEIN_DISTANCE.numberCall(left, right)

/**
 * Check whether the pattern search is contained in the string text, using wildcard matching.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#like
 */
@VaultFunctionMarker
fun LIKE(
    text: AqlExpression<String>,
    search: AqlExpression<String>,
): AqlExpression<Boolean> =
    AqlFunc.LIKE.boolCall(text, search)

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
    AqlFunc.LIKE.boolCall(text, search, caseInsensitive)

/**
 * Convert upper-case letters in value to their lower-case counterparts. All other characters are returned unchanged.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#lower
 */
@VaultFunctionMarker
fun LOWER(
    expr: AqlExpression<String>,
): AqlExpression<String> =
    AqlFunc.LOWER.stringCall(expr)

/**
 * Return the string value with whitespace stripped from the start only.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#ltrim
 */
@VaultFunctionMarker
fun LTRIM(
    subject: AqlExpression<String>,
): AqlExpression<String> =
    AqlFunc.LTRIM.stringCall(subject)

/**
 * Return the string value with whitespace stripped from the start only.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#ltrim
 */
@VaultFunctionMarker
fun LTRIM(
    subject: AqlExpression<String>,
    chars: AqlExpression<String>,
): AqlExpression<String> =
    AqlFunc.LTRIM.stringCall(subject, chars)

/**
 * Calculate the MD5 checksum for text and return it in a hexadecimal string representation.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#md5
 */
@VaultFunctionMarker
fun MD5(
    value: AqlExpression<String>,
): AqlExpression<String> =
    AqlFunc.MD5.stringCall(value)

/**
 * Generate a pseudo-random token string with the specified length. The algorithm for token generation should be treated as opaque.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#randomtoken
 */
@VaultFunctionMarker
fun <T : Number> RANDOM_TOKEN(
    length: AqlExpression<T>,
): AqlExpression<String> =
    AqlFunc.RANDOM_TOKEN.stringCall(length)

/**
 * Return the matches in the given string text, using the regex.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#regexmatches
 */
@VaultFunctionMarker
fun REGEX_MATCHES(
    text: AqlExpression<String>,
    regex: AqlExpression<String>,
): AqlExpression<List<String>?> =
    AqlFunc.REGEX_MATCHES.nullableArrayCall(type = kListType<String>().nullable, text, regex)

/**
 * Return the matches in the given string text, using the regex.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#regexmatches
 */
@VaultFunctionMarker
fun REGEX_MATCHES(
    text: AqlExpression<String>,
    regex: AqlExpression<String>,
    caseInsensitive: AqlExpression<Boolean>,
): AqlExpression<List<String>?> =
    AqlFunc.REGEX_MATCHES.nullableArrayCall(type = kListType<String>().nullable, text, regex, caseInsensitive)

/**
 * Return the matches in the given string text, using the regex.
 *
 * See https://docs.arangodb.com/stable/aql/functions/string/#regex_split
 */
@VaultFunctionMarker
fun REGEX_SPLIT(
    text: AqlExpression<String>,
    regex: AqlExpression<String>,
): AqlExpression<List<String>?> =
    AqlFunc.REGEX_SPLIT.nullableArrayCall(type = kListType<String>().nullable, text, regex)

/**
 * Return the matches in the given string text, using the regex.
 *
 * See https://docs.arangodb.com/stable/aql/functions/string/#regex_split
 */
@VaultFunctionMarker
fun REGEX_SPLIT(
    text: AqlExpression<String>,
    regex: AqlExpression<String>,
    caseInsensitive: AqlExpression<Boolean>,
): AqlExpression<List<String>?> =
    AqlFunc.REGEX_SPLIT.nullableArrayCall(type = kListType<String>().nullable, text, regex, caseInsensitive)

/**
 * Return the matches in the given string text, using the regex.
 *
 * See https://docs.arangodb.com/stable/aql/functions/string/#regex_split
 */
@VaultFunctionMarker
fun REGEX_SPLIT(
    text: AqlExpression<String>,
    regex: AqlExpression<String>,
    caseInsensitive: AqlExpression<Boolean>,
    limit: AqlExpression<Int>,
): AqlExpression<List<String>?> =
    AqlFunc.REGEX_SPLIT
        .nullableArrayCall(type = kListType<String>().nullable, text, regex, caseInsensitive, limit)

/**
 * Return the matches in the given string text, using the regex.
 *
 * See https://docs.arangodb.com/stable/aql/functions/string/#regex_test
 */
@VaultFunctionMarker
fun REGEX_TEST(
    text: AqlExpression<String>,
    regex: AqlExpression<String>,
): AqlExpression<Boolean?> =
    AqlFunc.REGEX_TEST.nullableBoolCall(text, regex)

/**
 * Return the matches in the given string text, using the regex.
 *
 * See https://docs.arangodb.com/stable/aql/functions/string/#regex_test
 */
@VaultFunctionMarker
fun REGEX_TEST(
    text: AqlExpression<String>,
    regex: AqlExpression<String>,
    caseInsensitive: AqlExpression<Boolean>,
): AqlExpression<Boolean?> =
    AqlFunc.REGEX_TEST.nullableBoolCall(text, regex, caseInsensitive)

/**
 * Return the reverse of the string value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#reverse
 */
@VaultFunctionMarker
fun REVERSE(
    value: AqlExpression<String>,
): AqlExpression<String> =
    AqlFunc.REVERSE.stringCall(value)

/**
 * Return the n rightmost characters of the string value.
 *
 * https://docs.arangodb.com/current/AQL/Functions/String.html#right
 */
@VaultFunctionMarker
fun <T : Number> RIGHT(
    value: AqlExpression<String>,
    n: AqlExpression<T>,
): AqlExpression<String> =
    AqlFunc.RIGHT.stringCall(value, n)

/**
 * Return the string value with whitespace stripped at the start only.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#rtrim
 */
@VaultFunctionMarker
fun RTRIM(
    subject: AqlExpression<String>,
): AqlExpression<String> =
    AqlFunc.RTRIM.stringCall(subject)

/**
 * Return the string value with whitespace stripped at the start only.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#rtrim
 */
@VaultFunctionMarker
fun RTRIM(
    subject: AqlExpression<String>,
    chars: AqlExpression<String>,
): AqlExpression<String> =
    AqlFunc.RTRIM.stringCall(subject, chars)

/**
 * Calculate the SHA1 checksum for text and returns it in a hexadecimal string representation.
 *
 * https://docs.arangodb.com/current/AQL/Functions/String.html#sha1
 */
@VaultFunctionMarker
fun SHA1(
    expr: AqlExpression<String>,
): AqlExpression<String> =
    AqlFunc.SHA1.stringCall(expr)

/**
 * Calculate the SHA512 checksum for text and returns it in a hexadecimal string representation.
 *
 * https://docs.arangodb.com/current/AQL/Functions/String.html#sha256
 */
@VaultFunctionMarker
fun SHA512(
    expr: AqlExpression<String>,
): AqlExpression<String> =
    AqlFunc.SHA512.stringCall(expr)

/**
 * Split the given string value into a list of strings, using the separator.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#split
 */
@VaultFunctionMarker
fun SPLIT(
    value: AqlExpression<String>,
    separator: AqlExpression<String>,
): AqlExpression<List<String>> =
    AqlFunc.SPLIT.arrayCall(type = kListType<String>(), value, separator)

/**
 * Split the given string value into a list of strings, using the separator.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#split
 */
@JvmName("SPLIT2")
@VaultFunctionMarker
fun SPLIT(
    value: AqlExpression<String>,
    separator: AqlExpression<List<String>>,
): AqlExpression<List<String>> =
    AqlFunc.SPLIT.arrayCall(type = kListType<String>(), value, separator)

/**
 * Split the given string value into a list of strings, using the separator.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#split
 */
@VaultFunctionMarker
fun <T : Number> SPLIT(
    value: AqlExpression<String>,
    separator: AqlExpression<String>,
    limit: AqlExpression<T>,
): AqlExpression<List<String>> =
    AqlFunc.SPLIT.arrayCall(type = kListType<String>(), value, separator, limit)

/**
 * Split the given string value into a list of strings, using the separator.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#split
 */
@JvmName("SPLIT2")
@VaultFunctionMarker
fun <T : Number> SPLIT(
    value: AqlExpression<String>,
    separator: AqlExpression<List<String>>,
    limit: AqlExpression<T>,
): AqlExpression<List<String>> =
    AqlFunc.SPLIT.arrayCall(type = kListType<String>(), value, separator, limit)

/**
 * Return the soundex fingerprint of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#soundex
 */
@VaultFunctionMarker
fun SOUNDEX(
    value: AqlExpression<String>,
): AqlExpression<String> =
    AqlFunc.SOUNDEX.stringCall(value)

/**
 * Return a substring of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#substring
 */
@VaultFunctionMarker
fun <T : Number> SUBSTRING(
    value: AqlExpression<String>,
    offset: AqlExpression<T>,
): AqlExpression<String> =
    AqlFunc.SUBSTRING.stringCall(value, offset)

/**
 * Return a substring of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#substring
 */
@VaultFunctionMarker
fun <T1 : Number, T2 : Number> SUBSTRING(
    value: AqlExpression<String>,
    offset: AqlExpression<T1>,
    length: AqlExpression<T2>,
): AqlExpression<String> =
    AqlFunc.SUBSTRING.stringCall(value, offset, length)

/**
 * Return the string value with whitespace stripped from start and end
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#trim
 */
@VaultFunctionMarker
fun TRIM(
    subject: AqlExpression<String>,
): AqlExpression<String> =
    AqlFunc.TRIM.stringCall(subject)

/**
 * Return the string value with whitespace stripped from start and end
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#trim
 */
@VaultFunctionMarker
fun TRIM(
    subject: AqlExpression<String>,
    chars: AqlExpression<String>,
): AqlExpression<String> =
    AqlFunc.TRIM.stringCall(subject, chars)

/**
 * Return the base64 representation of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#tobase64
 */
@VaultFunctionMarker
fun TO_BASE64(
    value: AqlExpression<String>,
): AqlExpression<String> =
    AqlFunc.TO_BASE64.stringCall(value)

/**
 * Return the hex representation of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#tobase64
 */
@VaultFunctionMarker
fun TO_HEX(
    value: AqlExpression<String>,
): AqlExpression<String> =
    AqlFunc.TO_HEX.stringCall(value)

/**
 * Convert lower-case letters in value to their upper-case counterparts. All other characters are returned unchanged.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#upper
 */
@VaultFunctionMarker
fun UPPER(
    expr: AqlExpression<String>,
): AqlExpression<String> =
    AqlFunc.UPPER.stringCall(expr)

/**
 * Return a universally unique identifier value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/String.html#uuid
 */
@VaultFunctionMarker
fun UUID(): AqlExpression<String> =
    AqlFunc.UUID.stringCall()

/**
 * Check whether the given string starts with prefix
 *
 * See https://www.arangodb.com/docs/stable/aql/functions-string.html#starts_with
 */
@VaultFunctionMarker
fun STARTS_WITH(
    text: AqlExpression<String>,
    prefix: AqlExpression<String>,
): AqlExpression<Boolean> =
    AqlFunc.STARTS_WITH.boolCall(text, prefix)
