package io.peekandpoke.kraft.addons.pdfjs

import io.peekandpoke.kraft.KraftDsl
import io.peekandpoke.kraft.addons.registry.Addon
import io.peekandpoke.kraft.addons.registry.AddonKey
import io.peekandpoke.kraft.addons.registry.AddonRegistry
import io.peekandpoke.kraft.addons.registry.AddonRegistryBuilder

/**
 * Facade for the lazily-loaded pdf.js addon.
 *
 * Wraps the [PdfJs] singleton, which loads the library from a CDN via ScriptLoader.
 */
class PdfJsAddon internal constructor(
    private val pdfJs: PdfJs,
) {
    /** Returns the underlying [PdfJs] instance for document loading. */
    fun instance(): PdfJs = pdfJs
}

/** Key for the pdfjs addon. */
val pdfJsAddonKey = AddonKey<PdfJsAddon>("pdfjs")

/** Registers the pdfjs addon for lazy loading via CDN script. */
@KraftDsl
fun AddonRegistryBuilder.pdfJs(lazy: Boolean = false): Addon<PdfJsAddon> = register(
    key = pdfJsAddonKey,
    name = "pdfjs",
    lazy = lazy,
) {
    PdfJsAddon(
        pdfJs = PdfJs.instance(),
    )
}

/** Accessor for the pdfjs addon on the registry. */
val AddonRegistry.pdfJs: Addon<PdfJsAddon>
    get() = this[pdfJsAddonKey]
