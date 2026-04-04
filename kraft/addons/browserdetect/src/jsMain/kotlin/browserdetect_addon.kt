package io.peekandpoke.kraft.addons.browserdetect

import io.peekandpoke.kraft.KraftDsl
import io.peekandpoke.kraft.addons.registry.Addon
import io.peekandpoke.kraft.addons.registry.AddonKey
import io.peekandpoke.kraft.addons.registry.AddonRegistry
import io.peekandpoke.kraft.addons.registry.AddonRegistryBuilder
import kotlinx.coroutines.await
import kotlin.js.Promise

/**
 * Facade for the lazily-loaded bowser addon.
 *
 * Provides type-safe access to browser detection via the bowser library.
 */
class BrowserDetectAddon internal constructor(
    private val bowserModule: dynamic,
) {
    /** Creates a bowser parser for the given [userAgent]. */
    fun getParser(userAgent: String): dynamic {
        val mod = bowserModule
        return mod.getParser(userAgent)
    }
}

/** Key for the browserdetect addon. */
val browserDetectAddonKey = AddonKey<BrowserDetectAddon>("browserdetect")

/** Registers the browserdetect addon for lazy loading via dynamic import. */
@KraftDsl
fun AddonRegistryBuilder.browserDetect(lazy: Boolean = false): Addon<BrowserDetectAddon> = register(
    key = browserDetectAddonKey,
    name = "browserdetect",
    lazy = lazy,
) {
    @Suppress("UnsafeCastFromDynamic")
    val module: dynamic = (js("import('bowser')") as Promise<dynamic>).await()

    BrowserDetectAddon(
        bowserModule = module.default ?: module,
    )
}

/** Accessor for the browserdetect addon on the registry. */
val AddonRegistry.browserDetect: Addon<BrowserDetectAddon>
    get() = this[browserDetectAddonKey]
