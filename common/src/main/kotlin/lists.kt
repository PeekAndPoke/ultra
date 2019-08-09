package io.ultra.common

fun <T> Collection<T>.containsAny(vararg element: T) = element.any { it in this }

fun <T> MutableList<T>.push(vararg element: T) : MutableList<T> = apply { addAll(element) }

fun <T> MutableList<T>.pop() : T? = if (size > 0) removeAt(size - 1) else null

fun <T> MutableList<T>.unshift(vararg element: T) : MutableList<T> = apply { addAll(0, element.toList()) }

fun <T> MutableList<T>.shift() : T? = if (size > 0) removeAt(0) else null
