package de.peekandpoke.ultra.slumber

import de.peekandpoke.ultra.common.TypedAttributes
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

    fun <T : Any> awake(type: KClass<T>, data: Any?, attributes: TypedAttributes = TypedAttributes.empty): T {

        val kType = type.createType(
            type.typeParameters.map { KTypeProjection.invariant(it.upperBounds[0]) }
        )

        return awake(kType, data, attributes)
    }

    fun <T> awake(type: KType, data: Any?, attributes: TypedAttributes = TypedAttributes.empty): T {

        val context = Awaker.Context(attributes)

        @Suppress("UNCHECKED_CAST")
        return config.getAwaker(type).awake(data, context) as T?
            ?: throw AwakerException("Could not awake '${type}'")
    }

    fun <T : Any> slumber(data: T): Any? {
        return config.getSlumberer(data::class).slumber(data)
    }
}
