package io.peekandpoke.kraft.addons.registry

/**
 * Type-safe key for identifying an addon in the [AddonRegistry].
 *
 * Each addon module defines a singleton key, e.g.:
 * ```
 * val markedAddonKey = AddonKey<MarkedAddon>("marked")
 * ```
 */
class AddonKey<T>(val name: String) {
    override fun equals(other: Any?) = other is AddonKey<*> && name == other.name
    override fun hashCode() = name.hashCode()
    override fun toString() = "AddonKey($name)"
}
