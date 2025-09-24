package de.peekandpoke.kraft.addons.signaturepad

import de.peekandpoke.kraft.addons.signaturepad.js.SignaturePadJs
import de.peekandpoke.kraft.addons.signaturepad.js.trim_canvas
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.debugId
import de.peekandpoke.kraft.utils.jsObject
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.common.model.FileBase64
import de.peekandpoke.ultra.semanticui.css
import kotlinx.browser.window
import kotlinx.css.height
import kotlinx.css.pct
import kotlinx.css.width
import kotlinx.html.Tag
import kotlinx.html.canvas
import kotlinx.html.div
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.Event

@Suppress("FunctionName")
fun Tag.SignaturePad(
    options: SignaturePadJs.Options = jsObject { },
    onChange: (SignaturePad) -> Unit = {},
) = comp(
    SignaturePad.Props(
        options = options,
        onChange = onChange,
    )
) {
    SignaturePad(it)
}

class SignaturePad(ctx: Ctx<Props>) : Component<SignaturePad.Props>(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val options: SignaturePadJs.Options,
        val onChange: (SignaturePad) -> Unit,
    )

    class Export(canvas: Lazy<HTMLCanvasElement?>) {
        val canvas by canvas

        /**
         * Brings the [Export] into scope.
         */
        operator fun invoke(block: Export.() -> Unit) {
            this.block()
        }

        /**
         * Exports the content as a [FileBase64].
         */
        fun toDataUrl(mimeType: String, quality: Any? = null): FileBase64? {
            return canvas?.toDataURL(mimeType, quality)?.let { FileBase64.fromDataUrl(it) }
        }

        /**
         * Exports the content as PNG as a [FileBase64].
         */
        fun toPng(): FileBase64? = toDataUrl("image/png")

        /**
         * Exports the content as SVG as a [FileBase64].
         */
        fun toSvg(): FileBase64? = toDataUrl("image/svg+xml")

        /**
         * Exports the content as JPG as a [FileBase64].
         */
        fun toJpg(quality: Double = 0.9): FileBase64? = toDataUrl("image/jpeg", quality)
    }

    private var pad: SignaturePadJs? = null

    //  Public interface  ///////////////////////////////////////////////////////////////////////////////////////

    val export get() = Export(lazy { getCanvas() })
    val trimmed get() = Export(lazy { getCanvasTrimmed() })

    fun clear() {
        pad?.clear()
        onChange()
    }

    fun isEmpty(): Boolean {
        return pad?.isEmpty() ?: false
    }

    fun isNotEmpty(): Boolean {
        return !isEmpty()
    }

    // Life-Cycle /////////////////////////////////////////////////////////////////////////////////////////

    init {
        lifecycle {
            onMount {
                window.addEventListener("resize", ::onWindowResize)

                dom?.let {
                    getCanvas()?.let { canvas ->
                        console.log("signature-pad", ::SignaturePadJs)

                        pad = SignaturePadJs(canvas, props.options)

                        pad?.addEventListener("endStroke", ::onPadEndStroke)

                        resize()
                    }
                }
            }

            onUnmount {
                window.removeEventListener("resize", ::onWindowResize)

                pad?.let {
                    it.removeEventListener("endStroke", ::onPadEndStroke)
                    it.off()
                }
            }
        }
    }

    // Rendering /////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        div {
            debugId("signature-pad")

            css {
                width = 100.pct
                height = 100.pct
            }

            canvas { }
        }
    }

    // Helpers /////////////////////////////////////////////////////////////////////////////////////////

    private fun getCanvas(): HTMLCanvasElement? {
        return dom?.querySelector("canvas") as? HTMLCanvasElement
    }

    private fun getCanvasTrimmed(): HTMLCanvasElement? {

        return getCanvas()?.let { original ->

            val cloned = original.cloneNode() as HTMLCanvasElement

            (cloned.getContext("2d") as CanvasRenderingContext2D).drawImage(original, 0.0, 0.0)

            trim_canvas.trimCanvas(cloned)
        }
    }

    private fun resize() {
        dom?.let { dom ->
            getCanvas()?.let { canvas ->

                canvas.width = dom.offsetWidth
                canvas.height = dom.offsetHeight

                pad?.clear()
            }
        }
    }

    private fun onChange() {
        props.onChange(this)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onWindowResize(evt: Event) {
        resize()
        onChange()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onPadEndStroke(evt: Event) {
        onChange()
    }
}
