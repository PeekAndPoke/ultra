package de.peekandpoke.ultra.common

inline fun <reified T : Enum<T>> safeEnumValueOf(value: String?, default: T): T {

    return when (value) {
        null -> default

        else -> try {
            enumValueOf(value)
        } catch (e: RuntimeException) {
            default
        }
    }
}
