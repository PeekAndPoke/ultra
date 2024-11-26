package de.peekandpoke.ultra.common.reflection

import kotlin.reflect.KClass

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

fun KClass<*>.isPrimitive() = this in primitiveTypes
