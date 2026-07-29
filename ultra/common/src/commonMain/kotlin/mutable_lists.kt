package io.peekandpoke.ultra.common

import kotlin.jvm.JvmName

/**
 * Adds the given [elements] to the end of the [MutableList]
 *
 * The receiver is modified in place. Adding nothing is a no-op.
 *
 * @return the same instance
 */
fun <T> MutableList<T>.push(vararg elements: T): MutableList<T> = push(elements)

/**
 * Adds the given [elements] to the end of the [MutableList]
 *
 * The receiver is modified in place. Adding nothing is a no-op.
 *
 * @return the same instance
 */
@JvmName("pushArray")
fun <T> MutableList<T>.push(elements: Array<out T>): MutableList<T> = push(elements.toList())

/**
 * Adds the given [elements] to the end of the [MutableList]
 *
 * The receiver is modified in place, keeping the iteration order of [elements].
 * Adding nothing is a no-op.
 *
 * @return the same instance
 */
@JvmName("pushCollection")
fun <T> MutableList<T>.push(elements: Collection<T>): MutableList<T> = apply { addAll(elements) }

/**
 * Removes and return the last element of the [MutableList]
 *
 * If there is no entry in the list then null is returned.
 *
 * On a nullable [T] an empty list and a list ending in `null` both yield null, so the result
 * alone does not tell whether anything was removed - check `isEmpty()` first if that matters.
 */
fun <T> MutableList<T>.pop(): T? = when {
    isNotEmpty() -> removeAt(size - 1)
    else -> null
}

/**
 * Adds the given [elements] to the start of the [MutableList]
 *
 * The receiver is modified in place. Adding nothing is a no-op.
 *
 * @return the same instance
 */
fun <T> MutableList<T>.unshift(vararg elements: T): MutableList<T> = unshift(elements)

/**
 * Adds the given [elements] to the start of the [MutableList]
 *
 * The receiver is modified in place. Adding nothing is a no-op.
 *
 * @return the same instance
 */
@JvmName("unshiftArray")
fun <T> MutableList<T>.unshift(elements: Array<out T>): MutableList<T> = unshift(elements.toList())

/**
 * Adds the given [elements] to the start of the [MutableList]
 *
 * The receiver is modified in place. The [elements] keep their relative order at the front,
 * so `[c].unshift([a, b])` becomes `[a, b, c]`. Adding nothing is a no-op.
 *
 * @return the same instance
 */
@JvmName("unshiftCollection")
fun <T> MutableList<T>.unshift(elements: Collection<T>): MutableList<T> = apply { addAll(0, elements) }

/**
 * Removes and returns the first element of the [MutableList]
 *
 * If there is no entry in the list then null is returned.
 *
 * On a nullable [T] an empty list and a list starting with `null` both yield null, so the result
 * alone does not tell whether anything was removed - check `isEmpty()` first if that matters.
 */
fun <T> MutableList<T>.shift(): T? = if (isNotEmpty()) removeAt(0) else null
