package io.peekandpoke.kraft.addons.prismjs

import io.peekandpoke.kraft.KraftDsl
import io.peekandpoke.kraft.addons.registry.Addon
import io.peekandpoke.kraft.addons.registry.AddonKey
import io.peekandpoke.kraft.addons.registry.AddonRegistry
import io.peekandpoke.kraft.addons.registry.AddonRegistryBuilder

/**
 * Facade for the lazily-loaded PrismJS addon.
 *
 * Ensures the core prismjs library is loaded via dynamic import.
 * Language grammars and plugins are loaded on demand by [PrismJsInternals].
 */
class PrismJsAddon internal constructor()

/** Key for the prismjs addon. */
val prismJsAddonKey = AddonKey<PrismJsAddon>("prismjs")

/** Registers the prismjs addon for loading via dynamic import. */
@KraftDsl
fun AddonRegistryBuilder.prismJs(lazy: Boolean = false): Addon<PrismJsAddon> = register(
    key = prismJsAddonKey,
    name = "prismjs",
    lazy = lazy,
) {
    prismJsInternals.core.ensureLoaded()

    PrismJsAddon()
}

/** Accessor for the prismjs addon on the registry. */
val AddonRegistry.prismJs: Addon<PrismJsAddon>
    get() = this[prismJsAddonKey]
