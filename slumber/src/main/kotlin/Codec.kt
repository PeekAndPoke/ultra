package de.peekandpoke.ultra.slumber

import kotlin.reflect.KClass
import kotlin.reflect.full.createType

class Codec(private val shared: Shared) {

    fun <T : Any> awake(type: KClass<T>, data: Any?): T {

        @Suppress("UNCHECKED_CAST")
        return shared.getAwaker(type.createType()).awake(data) as T?
            ?: throw AwakerException("Could not awake '${type.qualifiedName}'")
    }

    fun <T : Any> slumber(data: T): Any {

        @Suppress("UNCHECKED_CAST")
        return shared.getSlumberer(data::class.createType()).slumber(data)
            ?: throw AwakerException("Could not slumber '${data::class.qualifiedName}'")
    }
}
