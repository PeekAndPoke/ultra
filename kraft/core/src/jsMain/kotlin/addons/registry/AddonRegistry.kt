package io.peekandpoke.kraft.addons.registry

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.getAttributeRecursive
import io.peekandpoke.ultra.common.TypedKey

/**
 * Registry holding all addons configured in `kraftApp { addons { ... } }`.
 *
 * Access from a component via:
 * ```
 * val marked by subscribingTo(addons.marked.ready)
 * ```
 */
class AddonRegistry constructor(
    private val addons: Map<AddonKey<*>, Addon<*>>,
) {
    companion object {
        val key = TypedKey<AddonRegistry>("AddonRegistry")

        /** Extension property for accessing the addon registry from a component. */
        val Component<*>.addons: AddonRegistry get() = getAttributeRecursive(key)
    }

    /** Gets a typed addon by its key. Throws if not registered. */
    operator fun <T> get(addonKey: AddonKey<T>): Addon<T> {
        @Suppress("UNCHECKED_CAST")
        return (addons[addonKey] as? Addon<T>)
            ?: error("Addon '${addonKey.name}' is not registered. Add it in kraftApp { addons { ${addonKey.name}() } }")
    }

    /** Gets a typed addon by its key, or null if not registered. */
    fun <T> getOrNull(addonKey: AddonKey<T>): Addon<T>? {
        @Suppress("UNCHECKED_CAST")
        return addons[addonKey] as? Addon<T>
    }

    /** Triggers loading of all non-lazy addons. Called after the registry is built. */
    fun loadEager() {
        addons.values.forEach { addon ->
            addon.load()
        }
    }
}
