package de.peekandpoke.funktor.rest.codegen

import kotlin.reflect.KClass

@DslMarker
annotation class CodeGenDsl

fun KClass<*>.joinSimpleNames(): String {
    return this.qualifiedName!!
        .replace(this.java.`package`.name, "")
        .replace(".", "")
}
