package de.peekandpoke.kraft.examples.jsaddons.signaturepad

import de.peekandpoke.kraft.addons.signaturepad.SignaturePad
import de.peekandpoke.kraft.components.*
import de.peekandpoke.kraft.semanticui.css
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.common.model.FileBase64
import kotlinx.css.*
import kotlinx.html.Tag
import kotlinx.html.div
import kotlinx.html.img

@Suppress("FunctionName")
fun Tag.SignaturePadExample() = comp {
    SignaturePadExample(it)
}

class SignaturePadExample(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var dataPng: FileBase64? by value(null)
    private var dataPngTrimmed: FileBase64? by value(null)
    private var dataSvg: FileBase64? by value(null)
    private var dataSvgTrimmed: FileBase64? by value(null)
    private var dataJpg: FileBase64? by value(null)
    private var dataJpgTrimmed: FileBase64? by value(null)

    private val sigPadRef = ComponentRef.Tracker<SignaturePad>()

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.segment {
            ui.header H2 { +"Signature Pad" }

            div {
                css {
                    position = Position.relative
                    border = Border(2.px, BorderStyle.dashed, Color.lightGrey)
                    backgroundColor = Color.white
                    height = 30.vh
                }

                sigPadRef { sigPad ->
                    div {
                        css {
                            position = Position.absolute
                            right = 1.px
                            bottom = 1.px
                        }

                        ui.tertiary.given(sigPad.isEmpty()) { disabled }.icon.button {
                            onClick {
                                sigPad.clear()
                            }
                            icon.eraser()
                        }
                    }
                }

                SignaturePad {
                    it.export {
                        dataPng = toPng()
                        dataSvg = toSvg()
                        dataJpg = toJpg(0.5)
                    }

                    it.trimmed {
                        dataPngTrimmed = toPng()
                        dataSvgTrimmed = toSvg()
                        dataJpgTrimmed = toJpg(0.5)
                    }
                }.track(sigPadRef)
            }

            sigPadRef { pad ->

                ui.divider {}

                ui.button {
                    onClick { pad.clear() }
                    icon.times()
                    +"Clear"
                }

                if (pad.isEmpty()) {
                    ui.red.label { +"Empty" }
                } else {
                    ui.green.label { +"Not empty" }
                }
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
}
