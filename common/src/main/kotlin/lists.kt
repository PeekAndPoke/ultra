package de.peekandpoke.ultra.common

fun <T> Collection<T>.containsAny(vararg elements: T) = containsAny(elements)

@JvmName("containsAnyArray")
fun <T> Collection<T>.containsAny(elements: Array<out T>) = elements.any { it in this }

@JvmName("containsAnyList")
fun <T> Collection<T>.containsAny(elements: List<T>) = elements.any { it in this }

fun <T> MutableList<T>.push(vararg elements: T): MutableList<T> = push(elements)

@JvmName("pushArray")
fun <T> MutableList<T>.push(elements: Array<out T>): MutableList<T> = apply { addAll(elements) }

fun <T> MutableList<T>.pop() : T? = if (size > 0) removeAt(size - 1) else null

fun <T> MutableList<T>.unshift(vararg elements: T): MutableList<T> = unshift(elements)

@JvmName("unshiftArray")
fun <T> MutableList<T>.unshift(elements: Array<out T>): MutableList<T> = apply { addAll(0, elements.toList()) }

fun <T> MutableList<T>.shift() : T? = if (size > 0) removeAt(0) else null
