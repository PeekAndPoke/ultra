package io.peekandpoke.ultra.common

/**
 * Safely converts a string [input] to an enum value of type [T], returning the [default] if the input
 * is null or does not match any enum constant.
 *
 * Matching is on the exact constant name and case-sensitive.
 */
inline fun <reified T : Enum<T>> safeEnumOf(input: String?, default: T): T {
    return safeEnumOrNull<T>(input) ?: default
}

/**
 * Safely converts a string [input] to an enum value of type [T], returning null if the input
 * is null or does not match any enum constant.
 *
 * Matching is on the exact constant name and case-sensitive. A miss is a plain lookup miss — this
 * does not raise and catch an exception on the way, which used to make a miss the expensive case on
 * request-supplied input.
 */
inline fun <reified T : Enum<T>> safeEnumOrNull(input: String?): T? {
    return when (input) {
        null -> null

        else -> enumValues<T>().firstOrNull { it.name == input }
    }
}

/**
 * Splits the string [input] by the [delimiter] and safely converts each part to an enum value of type [T].
 *
 * Parts that do not match any enum constant are silently ignored.
 * Returns an empty list when [input] is null.
 *
 * Parts are trimmed and blank parts are dropped. Repeats are kept, so `"A,A,B"` yields three values
 * in input order — de-duplicate afterwards if that is what you want.
 */
inline fun <reified T : Enum<T>> safeEnumsOf(input: String?, delimiter: String = ","): List<T> {
    return when (input) {
        null -> emptyList()

        else -> input.split(delimiter)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { safeEnumOrNull<T>(it) }
    }
}
