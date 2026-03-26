package io.peekandpoke.kraft.components

import io.peekandpoke.ultra.common.TypedKey

/** Gets the attribute for the given [key], or null if not set. */
fun <T> Component<*>.getAttributeOrNull(key: TypedKey<T>): T? {
    return attributes[key]
}

/** Gets the attribute for the given [key], throwing if not set. */
fun <T> Component<*>.getAttribute(key: TypedKey<T>): T {
    return getAttributeOrNull(key)
        ?: throw IllegalStateException("No attribute set for key '${key.name}'")
}

/** Gets the attribute for the given [key], searching up the component tree. Returns null if not found. */
fun <T> Component<*>.getAttributeRecursiveOrNull(key: TypedKey<T>): T? {
    return getAttributeOrNull(key)
        ?: parent?.getAttributeRecursiveOrNull(key)
}

/** Gets the attribute for the given [key], searching up the component tree. Throws if not found. */
fun <T> Component<*>.getAttributeRecursive(key: TypedKey<T>): T {
    return getAttributeRecursiveOrNull(key)
        ?: throw IllegalStateException("No attribute set for key '${key.name}'")
}
