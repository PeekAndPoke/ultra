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
 * Removes and returns the last element, or null when the list is empty.
 */
@Deprecated(
    message = "Replaced by the stdlib. Note both share the caveat that null cannot distinguish " +
            "an empty list from a stored null.",
    replaceWith = ReplaceWith("removeLastOrNull()"),
    level = DeprecationLevel.ERROR,
)
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
 * Removes and returns the first element, or null when the list is empty.
 */
@Deprecated(
    message = "Replaced by the stdlib. Note both share the caveat that null cannot distinguish " +
            "an empty list from a stored null.",
    replaceWith = ReplaceWith("removeFirstOrNull()"),
    level = DeprecationLevel.ERROR,
)
fun <T> MutableList<T>.shift(): T? = if (isNotEmpty()) removeAt(0) else null
