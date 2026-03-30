package io.peekandpoke.ultra.reflection

import kotlin.reflect.KClass

/**
 * The set of Kotlin primitive types.
 *
 * Includes [String], [Byte], [Short], [Int], [Long], [Float], [Double], and [Boolean].
 */
val primitiveTypes = setOf(
    String::class,
    Byte::class,
    Short::class,
    Int::class,
    Long::class,
    Float::class,
    Double::class,
    Boolean::class,
)

/**
 * Returns `true` if this [KClass] is one of the [primitiveTypes].
 */
fun KClass<*>.isPrimitive() = this in primitiveTypes
