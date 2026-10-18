package io.peekandpoke.ultra.slumber

import io.peekandpoke.ultra.common.TypedAttributes
import io.peekandpoke.ultra.slumber.builtin.BuiltInModule
import io.peekandpoke.ultra.slumber.builtin.datetime.javatime.JavaTimeModule
import io.peekandpoke.ultra.slumber.builtin.datetime.kotlinx.KotlinxTimeModule
import io.peekandpoke.ultra.slumber.builtin.datetime.mp.MpDateTimeModule
import kotlin.reflect.KType

/**
 * Configuration for a [Codec], defining which [SlumberModule]s handle which types.
 *
 * Modules are queried in order — the first module to return a non-null [Awaker] or [Slumberer] wins.
 * Results are cached in [Lookup]. [appendModules] and [prependModules] start from a fresh [Lookup];
 * every other derivation (including `copy`) keeps the existing one.
 */
// TODO(scan): `lookup` sits in the primary ctor of a data class, so it takes part in equals/hashCode
//  (by identity, Lookup has no equals) -- two configs built from the same modules are never equal.
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
    // TODO(scan): plain mutable maps, written by getAwaker/getSlumberer, and one instance is shared by
    //  every config derived through `copy`/[plusAttributes] -- concurrent readers of one [Codec] write
    //  the same LinkedHashMap. A fix cannot simply use ConcurrentHashMap: null values are stored here.
    class Lookup {
        val awakers = mutableMapOf<KType, Awaker?>()
        val slumberers = mutableMapOf<KType, Slumberer?>()
    }

    /** Creates a [Codec] backed by this config. */
    fun codec(): Codec = Codec(this)

    /** Appends modules after the existing ones. Resets the lookup cache. */
    fun appendModules(vararg module: SlumberModule) = appendModules(module.toList())

    /** Appends modules after the existing ones. Resets the lookup cache. */
    // TODO(scan): does not pass `attributes`, so it silently resets them to TypedAttributes.empty --
    //  `config.plusAttributes(x).appendModules(m).attributes` is empty. Same in [prependModules].
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
    // TODO(scan): `copy` keeps the SAME [Lookup] instance, but attributes are handed to
    //  SlumberModule.getSlumberer and can change what it returns (DataClassSlumberer picks its cache
    //  from them) -- the derived config then inherits, and pollutes, codecs resolved under the old
    //  attributes. funktor works around this by hand: funktor/rest/src/jvmMain/kotlin/index_jvm.kt:62.
    fun plusAttributes(attributes: TypedAttributes): SlumberConfig = copy(
        attributes = this.attributes.plus(attributes)
    )

    /** Resolves the [Awaker] for [type] by querying modules in order. Results are cached. */
    // TODO(scan): `error(...)` raises IllegalStateException, not a [SlumberException] -- a caller
    //  catching the library's own exception type misses the unresolvable-type case. Same in
    //  [getSlumberer]. A null result is also re-resolved on every call (getOrPut stores it, then
    //  treats the stored null as a miss).
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
