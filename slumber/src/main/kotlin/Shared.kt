package de.peekandpoke.ultra.slumber

import de.peekandpoke.ultra.slumber.builtin.BuiltInModule
import kotlin.reflect.KType

class Shared {

    private val modules = listOf(BuiltInModule())

    private val awakerLookup = mutableMapOf<KType, Awaker?>()

    private val slumbererLookUp = mutableMapOf<KType, Slumberer?>()

    fun getAwaker(type: KType): Awaker {

        return awakerLookup.getOrPut(type) {
            modules.map { it.getAwaker(type, this) }.firstOrNull()
        }
            ?: error("There is no known way to awake the type '$type'")
    }

    fun getSlumberer(type: KType): Slumberer {

        return slumbererLookUp.getOrPut(type) {
            modules.map { it.getSlumberer(type, this) }.firstOrNull()
        }
            ?: error("There is no known way to slumber the type '$type'")
    }
}
