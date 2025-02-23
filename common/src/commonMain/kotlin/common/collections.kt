package de.peekandpoke.ultra.common

import kotlin.jvm.JvmName

/**
 * Returns 'true' if the collection contains any of the given [elements]
 */
@JvmName("containsAnyCollection")
fun <T> Collection<T>.containsAny(elements: Collection<T>): Boolean {
    val set = this.toSet()
    val elems = elements.toSet()

    val intersection = set.intersect(elems)

    return intersection.isNotEmpty()
}

/**
 * Returns 'true' if the collection contains any of the given [elements]
 */
@JvmName("containsAnyArray")
fun <T> Collection<T>.containsAny(elements: Array<out T>): Boolean = containsAny(elements.toList())

/**
 * Returns 'true' if the collection contains none of the given [elements]
 */
@JvmName("containsNoneCollection")
fun <T> Collection<T>.containsNone(elements: Collection<T>): Boolean = !containsAny(elements)

/**
 * Returns 'true' if the collection contains none of the given [elements]
 */
@JvmName("containsNoneArray")
fun <T> Collection<T>.containsNone(elements: Array<out T>): Boolean = !containsAny(elements)
