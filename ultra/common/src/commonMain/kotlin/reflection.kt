package io.peekandpoke.ultra.common

import kotlin.reflect.KType

/**
 * Returns `true` if this type represents a Kotlin primitive type
 * (Boolean, Char, Byte, Short, Int, Long, Float, or Double).
 */
val KType.isPrimitive
    get(): Boolean = classifier in listOf(
        Boolean::class,
        Char::class,
        Byte::class,
        Short::class,
        Int::class,
        Long::class,
        Float::class,
        Double::class
    )

/**
 * Returns `true` if this type represents a Kotlin primitive type or [String].
 */
val KType.isPrimitiveOrString get(): Boolean = classifier == String::class || isPrimitive
