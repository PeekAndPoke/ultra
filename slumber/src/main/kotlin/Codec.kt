package de.peekandpoke.ultra.slumber

import de.peekandpoke.ultra.slumber.builtin.BuiltInModule
import de.peekandpoke.ultra.slumber.builtin.DateTimeModule
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

open class Codec(private val config: Config) {

    companion object {
        val default = Codec(
            Config(
                listOf(
                    DateTimeModule(),
                    BuiltInModule()
                )
            )
        )
    }

    fun <T : Any> awake(type: KClass<T>, data: Any?): T {

        val kType = type.createType(
            type.typeParameters.map { KTypeProjection.invariant(it.upperBounds[0]) }
        )

        return awake(kType, data)
    }

    fun <T> awake(type: KType, data: Any?): T {

        @Suppress("UNCHECKED_CAST")
        return config.getAwaker(type).awake(data) as T?
            ?: throw AwakerException("Could not awake '${type}'")
    }

    fun <T : Any> slumber(data: T): Any? {
        return config.getSlumberer(data::class).slumber(data)
    }
}
