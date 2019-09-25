package de.peekandpoke.ultra.common

import java.net.URLEncoder

/**
 * Puts the string "with" before and after the string
 */
fun String.surround(with: String) = "$with${this}$with"

/**
 * Converts the first letter of the String to uppercase
 */
fun String.ucFirst(): String {

    if (length == 0) return this

    return substring(0, 1).toUpperCase() + substring(1)
}

/**
 * Converts the first letter of the String to lowercase
 */
fun String.lcFirst(): String {

    if (length == 0) return this

    return substring(0, 1).toLowerCase() + substring(1)
}

/**
 * Returns 'true' when the string starts with any of the given prefixes
 */
fun String.startsWithAny(vararg prefix: String) = prefix.any { startsWith(it) }

/**
 * Returns 'true' when the string starts with any of the given prefixes
 */
@JvmName("startsWithAnyArray")
fun String.startsWithAny(prefixes: Array<out String>) = prefixes.any { startsWith(it) }

/**
 * Returns 'true' when the string does NOT start with any of the given prefixes
 */
fun String.startsWithNone(vararg prefix: String) = !startsWithAny(prefix)

/**
 * Returns 'true' when the string does NOT start with any of the given prefixes
 */
@JvmName("startsWithNoneArray")
fun String.startsWithNone(prefixes: Array<out String>) = !startsWithAny(prefixes)

/**
 * Appends url parameters the string
 */
fun String.toUri(queryParams: Map<String, String>) = when {
    queryParams.isEmpty() -> this

    else -> "$this?" + queryParams
        .map { (k, v) -> "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}" }
        .joinToString("&")
}
