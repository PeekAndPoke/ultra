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
    val lookup: Lookup = Lookup(),
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

    class Lookup {
        val awakers = mutableMapOf<KType, Awaker?>()
        val slumberers = mutableMapOf<KType, Slumberer?>()
    }

    fun codec(): Codec = Codec(this)

    fun appendModules(vararg module: SlumberModule) = appendModules(module.toList())

    fun appendModules(append: List<SlumberModule>) = SlumberConfig(
        modules = this.modules.plus(append),
        lookup = Lookup(), // We reset the lookup, because the new module could handle types differently
    )

    fun prependModules(vararg module: SlumberModule) = prependModules(module.toList())

    fun prependModules(prepend: List<SlumberModule>) = SlumberConfig(
        modules = prepend.plus(this.modules),
        lookup = Lookup(), // We reset the lookup, because the new module could handle types differently
    )

    fun plusAttributes(attributes: TypedAttributes): SlumberConfig = copy(
        attributes = this.attributes.plus(attributes)
    )

    fun getAwaker(type: KType): Awaker {
        return lookup.awakers.getOrPut(type) {
            modules.asSequence()
                .mapNotNull { it.getAwaker(type, attributes) }
                .firstOrNull()
        }
            ?: error("There is no known way to awake the type '$type'")
    }

    fun getSlumberer(type: KType): Slumberer {

        return lookup.slumberers.getOrPut(type) {
            modules.asSequence()
                .mapNotNull { it.getSlumberer(type, attributes) }
                .firstOrNull()
        }
            ?: error("There is no known way to slumber the type '$type'")
    }
}
