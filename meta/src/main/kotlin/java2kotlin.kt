package de.peekandpoke.ultra.meta

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.INT

val ClassName.toKotlin
    get() = toKotlinCache.getOrPut(this) {
        when {

            this.canonicalName == "java.lang.Integer" -> INT
            this.canonicalName == "java.lang.Character" -> CHAR
            this.canonicalName == "java.lang.Object" -> ANY

            this.packageName in listOf("java.lang", "kotlin.jvm.functions") ->
                ClassName("kotlin", simpleNames)

            this.packageName == "java.util" -> ClassName("kotlin.collections", simpleNames)

            else -> this
        }
    }

private val toKotlinCache = mutableMapOf<ClassName, ClassName>()
