package io.peekandpoke.kraft.addons.signaturepad

import io.peekandpoke.kraft.KraftDsl
import io.peekandpoke.kraft.addons.registry.Addon
import io.peekandpoke.kraft.addons.registry.AddonKey
import io.peekandpoke.kraft.addons.registry.AddonRegistry
import io.peekandpoke.kraft.addons.registry.AddonRegistryBuilder
import io.peekandpoke.kraft.addons.signaturepad.js.SignaturePadJs
import kotlinx.coroutines.await
import org.w3c.dom.HTMLCanvasElement
import kotlin.js.Promise

/**
 * Facade for the lazily-loaded signature_pad + trim-canvas addon.
 *
 * Provides type-safe access to the SignaturePad JS library for capturing
 * handwritten signatures on an HTML canvas.
 */
class SignaturePadAddon internal constructor(
    private val signaturePadConstructor: dynamic,
    private val trimCanvasFn: dynamic,
) {
    /**
     * Creates a new SignaturePad instance on the given [canvas].
     *
     * @param canvas the HTML canvas element to attach to
     * @param options optional configuration (penColor, minWidth, maxWidth, etc.)
     * @return a [SignaturePadJs] instance
     */
    fun create(canvas: HTMLCanvasElement, options: SignaturePadJs.Options? = null): SignaturePadJs {
        // Local variable needed: js() blocks capture locals but not Kotlin's `this`.
        val ctor = signaturePadConstructor

        return if (options != null) {
            js("new ctor(canvas, options)").unsafeCast<SignaturePadJs>()
        } else {
            js("new ctor(canvas)").unsafeCast<SignaturePadJs>()
        }
    }

    /**
     * Trims whitespace from the edges of a canvas.
     *
     * @param canvas the canvas to trim
     * @return the trimmed canvas
     */
    fun trimCanvas(canvas: HTMLCanvasElement): HTMLCanvasElement {
        return trimCanvasFn(canvas).unsafeCast<HTMLCanvasElement>()
    }
}

/** Key for the signaturepad addon. */
val signaturePadAddonKey = AddonKey<SignaturePadAddon>("signaturepad")

/** Registers the signaturepad addon for lazy loading via dynamic import. */
@KraftDsl
fun AddonRegistryBuilder.signaturePad(lazy: Boolean = false): Addon<SignaturePadAddon> = register(
    key = signaturePadAddonKey,
    name = "signaturepad",
    lazy = lazy,
) {
    @Suppress("UnsafeCastFromDynamic")
    val sigPadModule: dynamic = (js("import('signature_pad')") as Promise<dynamic>).await()

    @Suppress("UnsafeCastFromDynamic")
    val trimModule: dynamic = (js("import('trim-canvas')") as Promise<dynamic>).await()

    SignaturePadAddon(
        signaturePadConstructor = sigPadModule.default ?: sigPadModule,
        trimCanvasFn = trimModule.default ?: trimModule,
    )
}

/** Accessor for the signaturepad addon on the registry. */
val AddonRegistry.signaturePad: Addon<SignaturePadAddon>
    get() = this[signaturePadAddonKey]
