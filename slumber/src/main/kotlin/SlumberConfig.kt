package de.peekandpoke.ultra.slumber

import de.peekandpoke.ultra.slumber.builtin.BuiltInModule
import de.peekandpoke.ultra.slumber.builtin.datetime.javatime.JavaTimeModule
import de.peekandpoke.ultra.slumber.builtin.datetime.kotlinx.KotlinxTimeModule
import de.peekandpoke.ultra.slumber.builtin.datetime.mp.MpDateTimeModule
import de.peekandpoke.ultra.slumber.builtin.datetime.portable.PortableTimeModule
import kotlin.reflect.KType

class SlumberConfig(val modules: List<SlumberModule> = listOf()) {

    companion object {
        val default = SlumberConfig(
            modules = listOf(
                MpDateTimeModule,
                JavaTimeModule,
                KotlinxTimeModule,
                PortableTimeModule,
                BuiltInModule,
            )
        )
    }

    private val awakerLookup = mutableMapOf<KType, Awaker?>()

    private val slumbererLookUp = mutableMapOf<KType, Slumberer?>()

    fun prependModules(vararg module: SlumberModule) = prependModules(module.toList())

    fun prependModules(prepend: List<SlumberModule>) = SlumberConfig(
        modules = prepend.plus(this.modules)
    )

    fun appendModules(vararg module: SlumberModule) = appendModules(module.toList())

    fun appendModules(append: List<SlumberModule>) = SlumberConfig(
        modules = this.modules.plus(append)
    )

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
