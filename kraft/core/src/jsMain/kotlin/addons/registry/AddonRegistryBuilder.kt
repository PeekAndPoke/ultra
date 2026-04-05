package io.peekandpoke.kraft.addons.registry

import io.peekandpoke.kraft.KraftDsl

/**
 * Builder for configuring addons in the `kraftApp { addons { ... } }` DSL.
 *
 * Each addon module provides an extension function on this builder, e.g.:
 * ```
 * fun AddonRegistryBuilder.marked(lazy: Boolean = false): Addon<MarkedAddon>
 * ```
 */
@KraftDsl
class AddonRegistryBuilder constructor() {
    private val addons = mutableMapOf<AddonKey<*>, Addon<*>>()

    /**
     * Registers an addon with the given [key], [name], and [loader].
     *
     * @param key    the unique key for this addon
     * @param name   human-readable name (used in error messages)
     * @param lazy   if true, the addon is loaded on first subscription; otherwise loaded eagerly
     * @param loader suspend function that performs the dynamic import and returns the addon facade
     * @return the [Addon] handle (useful for storing a reference)
     */
    fun <T> register(
        key: AddonKey<T>,
        name: String,
        lazy: Boolean = false,
        loader: suspend () -> T,
    ): Addon<T> {
        val addon = Addon(name = name, lazy = lazy, loader = loader)
        addons[key] = addon
        return addon
    }

    fun build(): AddonRegistry {
        return AddonRegistry(addons.toMap())
    }
}
