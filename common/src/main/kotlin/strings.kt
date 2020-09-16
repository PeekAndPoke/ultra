package de.peekandpoke.ultra.common

import java.net.URLEncoder

/**
 * Puts the string [with] before and after the string
 */
fun String.surround(with: String) = "$with${this}$with"

/**
 * Converts the first letter of the String to uppercase
 */
fun String.ucFirst(): String = when {
    isEmpty() -> this
    else -> substring(0, 1).toUpperCase() + substring(1)
}

/**
 * Converts the first letter of the String to lowercase
 */
fun String.lcFirst(): String = when {
    isEmpty() -> this
    else -> substring(0, 1).toLowerCase() + substring(1)
}

/**
 * Returns 'true' when the string starts with any of the given prefixes
 */
fun String.startsWithAny(vararg prefixes: String) = startsWithAny(prefixes)

/**
 * Returns 'true' when the string starts with any of the given prefixes
 */
@JvmName("startsWithAnyArray")
fun String.startsWithAny(prefixes: Array<out String>) = prefixes.any { startsWith(it) }

/**
 * Returns 'true' when the string starts with any of the given prefixes
 */
@JvmName("startsWithAnyCollection")
fun String.startsWithAny(prefixes: Collection<String>) = prefixes.any { startsWith(it) }

/**
 * Returns 'true' when the string does NOT start with any of the given prefixes
 */
fun String.startsWithNone(vararg prefixes: String) = !startsWithAny(prefixes)

/**
 * Returns 'true' when the string does NOT start with any of the given prefixes
 */
@JvmName("startsWithNoneArray")
fun String.startsWithNone(prefixes: Array<out String>) = !startsWithAny(prefixes)

/**
 * Returns 'true' when the string does NOT start with any of the given prefixes
 */
@JvmName("startsWithNoneCollection")
fun String.startsWithNone(prefixes: Collection<String>) = !startsWithAny(prefixes)

/**
 * Returns the maximal line length of a multiline string.
 *
 * The string is first split by the [separator] and then the max length is computed
 */
fun String.maxLineLength(separator: String = System.lineSeparator()): Int =
    split(separator).map { it.length }.maxOrNull() ?: 0

/**
 * Takes [maxLength] of the string and adds the [suffix] if the length is bigger than [maxLength]
 */
fun String.ellipsis(maxLength: Int = 50, suffix: String = "...") = when (length > maxLength) {
    true -> "${this.take(maxLength)}$suffix"
    else -> this
}

/**
 * Splits a camel cased word into single words
 */
fun String.camelCaseSplit() = camelCaseSplitRegex.split(this)

/**
 * Splits a camel case word and joins the parts using the divider
 */
fun String.camelCaseDivide(divider: String = " ") = camelCaseSplit().joinToString(divider)

/**
 * Appends url parameters the string
 */
fun String.toUri(queryParams: Map<String, String>) = when {
    queryParams.isEmpty() -> this

    else -> when {
        contains("?") -> "$this&"
        else -> "$this?"
    } + queryParams
        .map { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }
        .joinToString("&")
}

/**
 * Appends url parameters the string
 */
fun String.toUri(queryParams: List<Pair<String, String>>) = toUri(queryParams.toMap())

/**
 * Appends url parameters the string
 */
fun String.toUri(vararg queryParams: Pair<String, String>) = toUri(queryParams.toMap())
