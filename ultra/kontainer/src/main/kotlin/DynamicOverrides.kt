package io.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

/**
 * Holds runtime overrides for dynamic services when creating a [Kontainer] from a [KontainerBlueprint].
 */
class DynamicOverrides internal constructor(
    val overrides: Map<KClass<out Any>, () -> Any>
) {
    /** DSL builder for specifying dynamic service overrides */
    class Builder {
        private val overrides = mutableMapOf<KClass<out Any>, () -> Any>()

        internal fun build() = DynamicOverrides(overrides)

        /** Overrides a dynamic service resolved by reified type */
        inline fun <reified SRV : Any> with(noinline instance: () -> SRV) {
            with(SRV::class, instance)
        }

        /** Overrides a dynamic service resolved by [srv] class */
        fun <SRV : Any, IMPL : SRV> with(srv: KClass<SRV>, instance: () -> IMPL) {
            overrides[srv] = instance
        }
    }

    /** Merges this with [other] overrides, with [other] taking precedence */
    fun merge(other: DynamicOverrides): DynamicOverrides {
        return DynamicOverrides(this.overrides + other.overrides)
    }
}
