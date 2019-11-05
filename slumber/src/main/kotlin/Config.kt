package de.peekandpoke.ultra.slumber

import kotlin.reflect.KType

class Config(private val modules: List<SlumberModule> = listOf()) {

    private val awakerLookup = mutableMapOf<KType, Awaker?>()

    private val slumbererLookUp = mutableMapOf<KType, Slumberer?>()

    fun getAwaker(type: KType): Awaker {

        return awakerLookup.getOrPut(type) {
            modules
                .asSequence()
                .mapNotNull { it.getAwaker(type) }
                .firstOrNull()
        }
            ?: error("There is no known way to awake the type '$type'")
    }

    fun getSlumberer(type: KType): Slumberer {

        return slumbererLookUp.getOrPut(type) {
            modules
                .asSequence()
                .mapNotNull { it.getSlumberer(type) }
                .firstOrNull()
        }
            ?: error("There is no known way to slumber the type '$type'")
    }
}
