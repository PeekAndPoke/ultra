package de.peekandpoke.ultra.common

import kotlin.reflect.KClass

interface Lookup<T : Any> {

    fun <X : T> has(cls: KClass<X>): Boolean

    fun <X : T> get(cls: KClass<X>): X?

    fun all(): List<T>
}
