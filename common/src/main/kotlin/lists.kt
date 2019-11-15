package de.peekandpoke.ultra.common

/**
 * Returns 'true' if the collection contains any of the given [elements]
 */
fun <T> Collection<T>.containsAny(vararg elements: T): Boolean = containsAny(elements)

/**
 * Returns 'true' if the collection contains any of the given [elements]
 */
@JvmName("containsAnyArray")
fun <T> Collection<T>.containsAny(elements: Array<out T>): Boolean = elements.any { it in this }

/**
 * Returns 'true' if the collection contains any of the given [elements]
 */
@JvmName("containsAnyCollection")
fun <T> Collection<T>.containsAny(elements: Collection<T>): Boolean = elements.any { it in this }

/**
 * Adds the given [elements] to the end of the [MutableList]
 *
 * @return the same instance
 */
fun <T> MutableList<T>.push(vararg elements: T): MutableList<T> = push(elements)

/**
 * Adds the given [elements] to the end of the [MutableList]
 *
 * @return the same instance
 */
@JvmName("pushArray")
fun <T> MutableList<T>.push(elements: Array<out T>): MutableList<T> = apply { addAll(elements) }

/**
 * Removes and return the last element of the [MutableList]
 *
 * If there is no entry in the list then null is returned.
 */
fun <T> MutableList<T>.pop() : T? = if (size > 0) removeAt(size - 1) else null

/**
 * Adds the given [elements] to the start of the [MutableList]
 *
 * @return the same instance
 */
fun <T> MutableList<T>.unshift(vararg elements: T): MutableList<T> = unshift(elements)

/**
 * Adds the given [elements] to the start of the [MutableList]
 *
 * @return the same instance
 */
@JvmName("unshiftArray")
fun <T> MutableList<T>.unshift(elements: Array<out T>): MutableList<T> = apply { addAll(0, elements.toList()) }

/**
 * Removes and returns the first element of the [MutableList]
 *
 * If there is no entry in the list then null is returned.
 */
fun <T> MutableList<T>.shift() : T? = if (size > 0) removeAt(0) else null
