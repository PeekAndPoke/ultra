package de.peekandpoke.ultra.common

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "unused")
// // TODO: test me JVM
expect class WeakSet<E>() {
    val size: Int

    fun toSet(): Set<E>

    fun contains(element: E): Boolean

    fun add(element: E)

    fun remove(element: E)

    fun clear()
}

fun <E> WeakSet<E>.isEmpty() = size == 0

fun <E> WeakSet<E>.isNotEmpty() = !isEmpty()
