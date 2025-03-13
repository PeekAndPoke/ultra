package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

class DynamicOverrides internal constructor(
    val overrides: Map<KClass<out Any>, () -> Any>
) {
    class Builder {
        private val overrides = mutableMapOf<KClass<out Any>, () -> Any>()

        internal fun build() = DynamicOverrides(overrides)

        inline fun <reified SRV : Any> with(noinline instance: () -> SRV) {
            with(SRV::class, instance)
        }

        fun <SRV : Any, IMPL : SRV> with(srv: KClass<SRV>, instance: () -> IMPL) {
            overrides[srv] = instance
        }
    }
}
