package io.peekandpoke.ultra.common

/**
 * Safely converts a string [input] to an enum value of type [T], returning the [default] if the input
 * is null or does not match any enum constant.
 *
 * Matching is on the exact constant name and case-sensitive. A miss goes through a thrown and
 * caught exception, which is not free on hot paths.
 */
@Suppress("Detekt.TooGenericExceptionCaught")
inline fun <reified T : Enum<T>> safeEnumOf(input: String?, default: T): T {
    return when (input) {
        null -> default

        else -> try {
            enumValueOf(input)
        } catch (e: RuntimeException) {
            default
        }
    }
}

/**
 * Safely converts a string [input] to an enum value of type [T], returning null if the input
 * is null or does not match any enum constant.
 *
 * Matching is on the exact constant name and case-sensitive. A miss goes through a thrown and
 * caught exception, which is not free on hot paths.
 */
@Suppress("Detekt.TooGenericExceptionCaught")
inline fun <reified T : Enum<T>> safeEnumOrNull(input: String?): T? {
    return when (input) {
        null -> null

        else -> try {
            enumValueOf(input) as T
        } catch (e: RuntimeException) {
            null
        }
    }
}

/**
 * Splits the string [input] by the [delimiter] and safely converts each part to an enum value of type [T].
 *
 * Parts that do not match any enum constant are silently ignored.
 * Returns an empty list when [input] is null.
 *
 * Parts are trimmed and blank parts are dropped. Repeated parts collapse into one entry, so
 * `"A,A,B"` yields two values, not three. The order follows the first occurrence in [input].
 */
inline fun <reified T : Enum<T>> safeEnumsOf(input: String?, delimiter: String = ","): List<T> {
    return when (input) {
        null -> emptyList()

        else -> input.splitAndTrimToSet(delimiter).mapNotNull { safeEnumOrNull<T>(it) }
    }
}
