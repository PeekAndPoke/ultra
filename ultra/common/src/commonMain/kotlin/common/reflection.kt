package de.peekandpoke.ultra.common

import kotlin.reflect.KType

// TODO: test me
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

// TODO: test me
val KType.isPrimitiveOrString get(): Boolean = classifier == String::class || isPrimitive
