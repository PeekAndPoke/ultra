package io.peekandpoke.kraft.addons.pdfjs

import io.peekandpoke.kraft.addons.registry.Addon
import io.peekandpoke.kraft.addons.registry.AddonKey
import io.peekandpoke.kraft.addons.registry.AddonRegistry
import io.peekandpoke.kraft.addons.registry.AddonRegistryBuilder

/**
 * Facade for the lazily-loaded pdf.js addon.
 *
 * Wraps the [PdfJs] singleton, which loads the library from the npm package `pdfjs-dist`, its own chunk: at app
 * start, or on first use when registered with `lazy = true`.
 */
class PdfJsAddon internal constructor(
    private val pdfJs: PdfJs,
) {
    /** Returns the underlying [PdfJs] instance for document loading. */
    fun instance(): PdfJs = pdfJs
}

/** Key for the pdfjs addon. */
val pdfJsAddonKey = AddonKey<PdfJsAddon>("pdfjs")

/** Registers the pdfjs addon; with [lazy], pdf.js is loaded on first use. */
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
