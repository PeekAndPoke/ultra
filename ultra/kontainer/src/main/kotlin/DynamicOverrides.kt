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

        /** A copy: using the builder again later does not change the built overrides */
        internal fun build() = DynamicOverrides(overrides.toMap())

        /** Overrides a dynamic service resolved by reified type; [instance] runs at create, once per kontainer */
        inline fun <reified SRV : Any> with(noinline instance: () -> SRV) {
            with(SRV::class, instance)
        }

        /**
         * Overrides a dynamic service resolved by [srv] class; [instance] runs at create, once per kontainer.
         *
         * A class given again moves to the end, so the override given last wins.
         */
        fun <SRV : Any, IMPL : SRV> with(srv: KClass<SRV>, instance: () -> IMPL) {
            overrides.remove(srv)
            overrides[srv] = instance
        }
    }

    /** Merges this with [other] overrides, with [other] taking precedence: its classes move to the end */
    fun merge(other: DynamicOverrides): DynamicOverrides {
        return DynamicOverrides((this.overrides - other.overrides.keys) + other.overrides)
    }
}
