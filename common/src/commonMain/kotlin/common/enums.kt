package de.peekandpoke.ultra.common

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

inline fun <reified T : Enum<T>> safeEnumsOf(input: String?, delimiter: String = ","): List<T> {
    return when (input) {
        null -> emptyList()

        else -> input.splitAndTrimToSet(delimiter).mapNotNull { safeEnumOrNull<T>(it) }
    }
}
