package de.peekandpoke.ultra.slumber

import de.peekandpoke.ultra.common.TypedAttributes
import de.peekandpoke.ultra.slumber.builtin.BuiltInModule
import de.peekandpoke.ultra.slumber.builtin.datetime.javatime.JavaTimeModule
import de.peekandpoke.ultra.slumber.builtin.datetime.kotlinx.KotlinxTimeModule
import de.peekandpoke.ultra.slumber.builtin.datetime.mp.MpDateTimeModule
import kotlin.reflect.KType

/**
 * Configuration for a [Codec], defining which [SlumberModule]s handle which types.
 *
 * Modules are queried in order — the first module to return a non-null [Awaker] or [Slumberer] wins.
 * Results are cached in [Lookup] for performance. Modifying modules via [appendModules] or [prependModules]
 * resets the cache.
 */
data class SlumberConfig(
    val modules: List<SlumberModule> = listOf(),
    val attributes: TypedAttributes = TypedAttributes.empty,
    val lookup: Lookup = Lookup(),
) {
    companion object {
        /** Default config with MpDateTime, KotlinxTime, JavaTime, and BuiltIn modules. */
        val default = SlumberConfig(
            modules = listOf(
                MpDateTimeModule,
                KotlinxTimeModule,
                JavaTimeModule,
                BuiltInModule,
            )
        )
    }

    /** Cache for resolved [Awaker]s and [Slumberer]s per [KType]. Reset when modules change. */
    class Lookup {
        val awakers = mutableMapOf<KType, Awaker?>()
        val slumberers = mutableMapOf<KType, Slumberer?>()
    }

    /** Creates a [Codec] backed by this config. */
    fun codec(): Codec = Codec(this)

    /** Appends modules after the existing ones. Resets the lookup cache. */
    fun appendModules(vararg module: SlumberModule) = appendModules(module.toList())

    /** Appends modules after the existing ones. Resets the lookup cache. */
    fun appendModules(append: List<SlumberModule>) = SlumberConfig(
        modules = this.modules.plus(append),
        lookup = Lookup(), // We reset the lookup, because the new module could handle types differently
    )

    /** Prepends modules before the existing ones (higher priority). Resets the lookup cache. */
    fun prependModules(vararg module: SlumberModule) = prependModules(module.toList())

    /** Prepends modules before the existing ones (higher priority). Resets the lookup cache. */
    fun prependModules(prepend: List<SlumberModule>) = SlumberConfig(
        modules = prepend.plus(this.modules),
        lookup = Lookup(), // We reset the lookup, because the new module could handle types differently
    )

    /** Merges additional [TypedAttributes] into this config. */
    fun plusAttributes(attributes: TypedAttributes): SlumberConfig = copy(
        attributes = this.attributes.plus(attributes)
    )

    /** Resolves the [Awaker] for [type] by querying modules in order. Results are cached. */
    fun getAwaker(type: KType): Awaker {
        return lookup.awakers.getOrPut(type) {
            modules.asSequence()
                .mapNotNull { it.getAwaker(type, attributes) }
                .firstOrNull()
        }
            ?: error("There is no known way to awake the type '$type'")
    }

    /** Resolves the [Slumberer] for [type] by querying modules in order. Results are cached. */
    fun getSlumberer(type: KType): Slumberer {

        return lookup.slumberers.getOrPut(type) {
            modules.asSequence()
                .mapNotNull { it.getSlumberer(type, attributes) }
                .firstOrNull()
        }
            ?: error("There is no known way to slumber the type '$type'")
    }
}
