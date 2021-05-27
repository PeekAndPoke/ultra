@file:JvmName("StringKt")

package de.peekandpoke.ultra.common

import kotlin.jvm.JvmName

/**
 * Puts the string [with] before and after the string
 */
fun String.surround(with: String) = "$with${this}$with"

/**
 * Converts the first letter of the String to uppercase
 */
fun String.ucFirst(): String = when {
    isEmpty() -> this
    else -> substring(0, 1).uppercase() + substring(1)
}

/**
 * Converts the first letter of the String to lowercase
 */
fun String.lcFirst(): String = when {
    isEmpty() -> this
    else -> substring(0, 1).lowercase() + substring(1)
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
fun String.maxLineLength(separator: String = "\n"): Int =
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
fun String.camelCaseSplit(): List<String> {

    val trimmed = trim()

    if (trimmed.isEmpty()) {
        return emptyList()
    }

    val result = mutableListOf<String>()

    val aToZCaps = 'A'..'Z'

    var lastStart = 0

    for (i in 1 until trimmed.length) {
        val c = trimmed[i]

        if (c in aToZCaps) {
            result.add(
                trimmed.substring(lastStart, i).trim()
            )
            lastStart = i
        }
    }

    if (lastStart < trimmed.length) {
        result.add(
            trimmed.substring(lastStart).trim()
        )
    }

    return result
}

/**
 * Splits a camel case word and joins the parts using the divider
 */
fun String.camelCaseDivide(divider: String = " "): String = camelCaseSplit().joinToString(divider)
