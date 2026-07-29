@file:Suppress("Detekt.TooManyFunctions")
@file:JvmName("StringKt")

package io.peekandpoke.ultra.common

import kotlin.jvm.JvmName

/**
 * Puts the string [with] before and after the string
 */
fun String.surround(with: String) = "$with${this}$with"

/**
 * Puts the string [prefix] before and the string [suffix] after the string
 */
fun String.surround(prefix: String, suffix: String) = "$prefix${this}$suffix"

/**
 * Converts the first letter of the String to uppercase
 *
 * An empty string is returned as is. Full case mapping is applied, so the result may be longer than
 * the input (`"ßa"` becomes `"SSa"`).
 */
fun String.ucFirst(): String = when {
    isEmpty() -> this
    else -> substring(0, 1).uppercase() + substring(1)
}

/**
 * Converts the first letter of the String to lowercase
 *
 * An empty string is returned as is. Full case mapping is applied, so the result may be longer than
 * the input (`"İx"` gains a combining dot).
 */
fun String.lcFirst(): String = when {
    isEmpty() -> this
    else -> substring(0, 1).lowercase() + substring(1)
}

/**
 * Returns 'true' when the string starts with any of the given prefixes
 *
 * Without any prefix the answer is 'false' (and [startsWithNone] is correspondingly 'true').
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
 * The string is first split by the [separator] and then the max length is computed. An empty string
 * has length 0. Lengths are UTF-16 code units, not glyphs, and only the literal [separator] is
 * stripped — CRLF text therefore counts a trailing CR into every line.
 */
fun String.maxLineLength(separator: String = "\n"): Int =
    split(separator).map { it.length }.maxOrNull() ?: 0

/**
 * Takes [maxLength] of the string and adds the [suffix] if the length is bigger than [maxLength]
 *
 * The [suffix] is appended on top of [maxLength], so a truncated result is `maxLength + suffix.length`
 * long and can even exceed the input (`"ab".ellipsis(1)` is `"a..."`). Lengths are UTF-16 code units,
 * so cutting inside a surrogate pair leaves a broken half. A negative [maxLength] throws.
 */
fun String.ellipsis(maxLength: Int = 50, suffix: String = "...") = when (length > maxLength) {
    true -> "${this.take(maxLength)}$suffix"
    else -> this
}

/**
 * Splits a camel cased word into single words
 *
 * A new word starts at every ASCII `A`..`Z`, so acronyms fall apart (`"XMLParser"` gives
 * `["X", "M", "L", "Parser"]`) and non-ASCII capitals are not word boundaries at all. The input and
 * every part are trimmed; a blank input gives an empty list.
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

/**
 * Checks if the string is a url with a protocol, e.g. https://...
 *
 * Only `http` and `https` qualify, and [UrlWithProtocolRegex] must match the WHOLE string — a
 * surrounding sentence or stray whitespace makes this 'false'.
 */
fun String.isUrlWithProtocol(): Boolean {
    return UrlWithProtocolRegex.matches(this)
}

/**
 * Checks if the string is a valid email
 *
 * [EmailRegex] must match the WHOLE string, so leading or trailing whitespace makes this 'false'.
 *
 * Anything longer than [MAX_EMAIL_LENGTH] is rejected without matching. That is the correct answer —
 * RFC 5321 caps a forward-path there — and it also keeps [EmailRegex] away from the input lengths
 * where its nested repetitions exhaust the stack.
 */
fun String.isEmail(): Boolean {
    if (length > MAX_EMAIL_LENGTH) {
        return false
    }

    return EmailRegex.matches(this)
}

/**
 * Checks if the string is a valid slug / DNS label: lowercase letters/digits and hyphens (only
 * between alphanumerics), length 1..63. Useful for tenant slugs that double as subdomains.
 */
fun String.isSlug(): Boolean {
    return SlugRegex.matches(this)
}

/**
 * Characters that no identifier may contain: C0 controls, DEL, C1 controls, and the Unicode
 * line/paragraph separators.
 *
 * Shared deliberately by every id type (`UserId`, `OrgId`, …). Those ids get composed into
 * NUL-delimited composite keys (the CSRF signing string, the auth session cache key) and written into
 * log lines, so banning the whole set makes a forged key boundary or a forged log line impossible BY
 * CONSTRUCTION rather than by convention. One predicate, so a later id type cannot quietly ship a
 * weaker rule.
 *
 * Lives here rather than next to the id classes because it is the same kind of thing as [isEmail] and
 * [isSlug] — a format predicate — and because `ultra:common` is the one module every id-defining
 * module already depends on.
 */
fun Char.isForbiddenInId(): Boolean =
    code < 0x20 || code == 0x7F || code in 0x80..0x9F || this == '\u2028' || this == '\u2029'

/**
 * Splits the string, trims all and creates a set of the elements.
 *
 * Blank parts are dropped, so an empty or all-blank string gives an empty set. Duplicates collapse;
 * the set keeps first-occurrence order.
 */
fun String.splitAndTrimToSet(delimiter: String = ",") =
    split(delimiter)
        .filter { it.isNotBlank() }
        .map { it.trim() }
        .toSet()
