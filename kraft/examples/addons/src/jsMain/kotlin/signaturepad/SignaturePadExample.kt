package io.peekandpoke.kraft.examples.jsaddons.signaturepad

import generated.ExtractedCodeBlocks
import io.peekandpoke.kraft.addons.registry.AddonRegistry.Companion.addons
import io.peekandpoke.kraft.addons.signaturepad.SignaturePadAddon
import io.peekandpoke.kraft.addons.signaturepad.js.SignaturePadJs
import io.peekandpoke.kraft.addons.signaturepad.signaturePad
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.examples.jsaddons.helpers.HorizontalContentAndCode
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.css
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.model.FileBase64
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.browser.window
import kotlinx.css.Border
import kotlinx.css.BorderStyle
import kotlinx.css.Color
import kotlinx.css.Position
import kotlinx.css.backgroundColor
import kotlinx.css.border
import kotlinx.css.bottom
import kotlinx.css.height
import kotlinx.css.pct
import kotlinx.css.position
import kotlinx.css.px
import kotlinx.css.right
import kotlinx.css.vh
import kotlinx.css.width
import kotlinx.html.Tag
import kotlinx.html.canvas
import kotlinx.html.div
import kotlinx.html.img
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.Event

@Suppress("FunctionName")
fun Tag.SignaturePadExample() = comp {
    SignaturePadExample(it)
}

class SignaturePadExample(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    // <CodeBlock subscribing>
    private val sigPadAddon: SignaturePadAddon? by subscribingTo(addons.signaturePad)
    // </CodeBlock>

    private var pad: SignaturePadJs? = null

    private var dataPng: FileBase64? by value(null)
    private var dataPngTrimmed: FileBase64? by value(null)
    private var dataSvg: FileBase64? by value(null)
    private var dataSvgTrimmed: FileBase64? by value(null)
    private var dataJpg: FileBase64? by value(null)
    private var dataJpgTrimmed: FileBase64? by value(null)

    //  LIFECYCLE  //////////////////////////////////////////////////////////////////////////////////////////////

    init {
        lifecycle {
            onMount {
                window.addEventListener("resize", ::onWindowResize)
                tryCreatePad()
            }

            onUpdate {
                // The addon may arrive after mount — create the pad once the canvas is in the DOM.
                tryCreatePad()
            }

            onUnmount {
                window.removeEventListener("resize", ::onWindowResize)
                destroyPad()
            }
        }
    }

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        val addon = sigPadAddon

        ui.segment {
            ui.header H2 { +"Signature Pad (via AddonRegistry)" }

            if (addon == null) {
                ui.placeholder.segment {
                    ui.icon.header {
                        icon.spinner_loading()
                        +"Loading signature pad addon..."
                    }
                }
                return@segment
            }

            ui.dividing.header { +"Subscribing to the addon" }

            HorizontalContentAndCode(
                ExtractedCodeBlocks.signaturepad_SignaturePadExample_kt_subscribing,
            ) {
                ui.label { +"SignaturePadAddon is loaded via subscribingTo(addons.signaturePad)" }
            }

            ui.dividing.header { +"Signature Pad" }

            div {
                css {
                    position = Position.relative
                    border = Border(2.px, BorderStyle.dashed, Color.lightGrey)
                    backgroundColor = Color.white
                    height = 30.vh
                }

                div {
                    css {
                        position = Position.absolute
                        right = 1.px
                        bottom = 1.px
                    }

                    ui.tertiary.given(pad?.isEmpty() != false) { disabled }.icon.button {
                        onClick { clearPad() }
                        icon.eraser()
                    }
                }

                div {
                    css {
                        width = 100.pct
                        height = 100.pct
                    }

                    canvas { }
                }
            }

            ui.divider {}

            ui.button {
                onClick { clearPad() }
                icon.times()
                +"Clear"
            }

            if (pad?.isEmpty() != false) {
                ui.red.label { +"Empty" }
            } else {
                ui.green.label { +"Not empty" }
            }

            ui.divider {}

            ui.two.column.grid {
                listOf(
                    "PNG" to dataPng,
                    "PNG trimmed" to dataPngTrimmed,
                    "SVG" to dataSvg,
                    "SVG trimmed" to dataSvgTrimmed,
                    "JPG" to dataJpg,
                    "JPG trimmed" to dataJpgTrimmed,
                ).forEach { (name, data) ->
                    ui.column {
                        ui.header { +name }

                        data?.let {
                            ui.sub.header { +"${data.mimeType}" }

                            ui.fitted.image {
                                css {
                                    border = Border(width = 1.px, style = BorderStyle.dotted, color = Color.gray)
                                }

                                img { src = data.asDataUrl() }
                            }
                        }
                    }
                }
            }
        }
    }

    //  HELPERS  ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun tryCreatePad() {
        val addon = sigPadAddon ?: return
        val canvas = getCanvas() ?: return

        if (pad != null) return

        pad = addon.create(canvas).also { newPad ->
            newPad.addEventListener("endStroke", ::onPadEndStroke)
        }

        resizeCanvas()
    }

    private fun destroyPad() {
        pad?.let {
            it.removeEventListener("endStroke", ::onPadEndStroke)
            it.off()
        }
        pad = null
    }

    private fun clearPad() {
        pad?.clear()
        exportSignature()
        triggerRedraw()
    }

    private fun getCanvas(): HTMLCanvasElement? {
        return dom?.querySelector("canvas") as? HTMLCanvasElement
    }

    private fun resizeCanvas() {
        val canvas = getCanvas() ?: return
        val container = canvas.parentElement ?: return

        canvas.width = container.clientWidth
        canvas.height = container.clientHeight

        pad?.clear()
    }

    private fun exportSignature() {
        val canvas = getCanvas()
        val addon = sigPadAddon

        dataPng = canvas?.toDataURL("image/png")?.let { FileBase64.fromDataUrl(it) }
        dataSvg = canvas?.toDataURL("image/svg+xml")?.let { FileBase64.fromDataUrl(it) }
        dataJpg = canvas?.toDataURL("image/jpeg", 0.5)?.let { FileBase64.fromDataUrl(it) }

        if (canvas != null && addon != null) {
            val trimmed = cloneAndTrimCanvas(canvas, addon)
            dataPngTrimmed = trimmed?.toDataURL("image/png")?.let { FileBase64.fromDataUrl(it) }
            dataSvgTrimmed = trimmed?.toDataURL("image/svg+xml")?.let { FileBase64.fromDataUrl(it) }
            dataJpgTrimmed = trimmed?.toDataURL("image/jpeg", 0.5)?.let { FileBase64.fromDataUrl(it) }
        } else {
            dataPngTrimmed = null
            dataSvgTrimmed = null
            dataJpgTrimmed = null
        }
    }

    private fun cloneAndTrimCanvas(original: HTMLCanvasElement, addon: SignaturePadAddon): HTMLCanvasElement? {
        val cloned = original.cloneNode() as HTMLCanvasElement
        (cloned.getContext("2d") as CanvasRenderingContext2D).drawImage(original, 0.0, 0.0)
        return addon.trimCanvas(cloned)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onWindowResize(evt: Event) {
        resizeCanvas()
        exportSignature()
        triggerRedraw()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onPadEndStroke(evt: Event) {
        exportSignature()
        triggerRedraw()
    }
}