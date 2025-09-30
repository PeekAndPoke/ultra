package de.peekandpoke.kraft.components

import de.peekandpoke.ultra.common.TypedKey

fun <T> Component<*>.getAttributeOrNull(key: TypedKey<T>): T? {
    return attributes[key]
}

fun <T> Component<*>.getAttribute(key: TypedKey<T>): T {
    return getAttributeOrNull(key)
        ?: throw IllegalStateException("No attribute set for key '${key.name}'")
}

fun <T> Component<*>.getAttributeRecursiveOrNull(key: TypedKey<T>): T? {
    return getAttributeOrNull(key)
        ?: parent?.getAttributeRecursiveOrNull(key)
}

fun <T> Component<*>.getAttributeRecursive(key: TypedKey<T>): T {
    return getAttributeRecursiveOrNull(key)
        ?: throw IllegalStateException("No attribute set for key '${key.name}'")
}
