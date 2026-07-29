package io.peekandpoke.ultra.common

import kotlin.jvm.JvmName

/**
 * Returns 'true' if the collection contains any of the given [elements]
 *
 * Matching is by `equals`/`hashCode`, not by identity. An empty [elements] yields 'false'.
 */
@JvmName("containsAnyCollection")
fun <T> Collection<T>.containsAny(elements: Collection<T>): Boolean {
    if (isEmpty() || elements.isEmpty()) {
        return false
    }

    // hash the receiver once, then stop at the first hit rather than computing a full intersection
    val lookup = this as? Set<T> ?: toSet()

    return elements.any { it in lookup }
}

/**
 * Returns 'true' if the collection contains any of the given [elements]
 *
 * Matching is by `equals`/`hashCode`, not by identity. An empty [elements] yields 'false'.
 */
@JvmName("containsAnyArray")
fun <T> Collection<T>.containsAny(elements: Array<out T>): Boolean = containsAny(elements.toList())

/**
 * Returns 'true' if the collection contains none of the given [elements]
 *
 * Matching is by `equals`/`hashCode`, not by identity. An empty [elements] yields 'true'.
 */
@JvmName("containsNoneCollection")
fun <T> Collection<T>.containsNone(elements: Collection<T>): Boolean = !containsAny(elements)

/**
 * Returns 'true' if the collection contains none of the given [elements]
 *
 * Matching is by `equals`/`hashCode`, not by identity. An empty [elements] yields 'true'.
 */
@JvmName("containsNoneArray")
fun <T> Collection<T>.containsNone(elements: Array<out T>): Boolean = !containsAny(elements)
