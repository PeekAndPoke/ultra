package de.peekandpoke.ultra.slumber

import de.peekandpoke.ultra.slumber.builtin.BuiltInModule
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

class Config(modules: List<SlumberModule> = listOf()) {

    private val modules = modules.plus(BuiltInModule())

    private val awakerLookup = mutableMapOf<KType, Awaker?>()

    private val slumbererLookUp = mutableMapOf<KType, Slumberer?>()

    fun getAwaker(type: KType): Awaker {

        return awakerLookup.getOrPut(type) {
            modules.mapNotNull { it.getAwaker(type, this) }.firstOrNull()
        }
            ?: error("There is no known way to awake the type '$type'")
    }

    fun getSlumberer(type: KClass<*>): Slumberer {
        // TODO: we can cache this
        return getSlumberer(
            type.createType(
                type.typeParameters.map { KTypeProjection.invariant(it.upperBounds[0]) }
            )
        )
    }

    fun getSlumberer(type: KType): Slumberer {

        return slumbererLookUp.getOrPut(type) {
            modules.mapNotNull { it.getSlumberer(type, this) }.firstOrNull()
        }
            ?: error("There is no known way to slumber the type '$type'")
    }
}
