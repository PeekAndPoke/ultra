package de.peekandpoke.ultra.slumber

import de.peekandpoke.ultra.common.TypedAttributes
import de.peekandpoke.ultra.slumber.builtin.BuiltInModule
import de.peekandpoke.ultra.slumber.builtin.datetime.javatime.JavaTimeModule
import de.peekandpoke.ultra.slumber.builtin.datetime.kotlinx.KotlinxTimeModule
import de.peekandpoke.ultra.slumber.builtin.datetime.mp.MpDateTimeModule
import kotlin.reflect.KType

data class SlumberConfig(
    val modules: List<SlumberModule> = listOf(),
    val attributes: TypedAttributes = TypedAttributes.empty,
) {
    companion object {
        val default = SlumberConfig(
            modules = listOf(
                MpDateTimeModule,
                KotlinxTimeModule,
                JavaTimeModule,
                BuiltInModule,
            )
        )
    }

    private val awakerLookup = mutableMapOf<KType, Awaker?>()

    private val slumbererLookUp = mutableMapOf<KType, Slumberer?>()

    fun codec(): Codec = Codec(this)

    fun prependModules(vararg module: SlumberModule) = prependModules(module.toList())

    fun prependModules(prepend: List<SlumberModule>) = SlumberConfig(
        modules = prepend.plus(this.modules)
    )

    fun appendModules(vararg module: SlumberModule) = appendModules(module.toList())

    fun appendModules(append: List<SlumberModule>) = SlumberConfig(
        modules = this.modules.plus(append)
    )

    fun plusAttributes(attributes: TypedAttributes): SlumberConfig = copy(
        attributes = this.attributes.plus(attributes)
    )

    fun getAwaker(type: KType): Awaker {

        return awakerLookup.getOrPut(type) {
            modules
                .asSequence()
                .mapNotNull { it.getAwaker(type, attributes) }
                .firstOrNull()
        }
            ?: error("There is no known way to awake the type '$type'")
    }

    fun getSlumberer(type: KType): Slumberer {

        return slumbererLookUp.getOrPut(type) {
            modules
                .asSequence()
                .mapNotNull { it.getSlumberer(type, attributes) }
                .firstOrNull()
        }
            ?: error("There is no known way to slumber the type '$type'")
    }
}
