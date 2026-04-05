package io.peekandpoke.kraft.examples.jsaddons.avatars

import generated.ExtractedCodeBlocks
import io.peekandpoke.kraft.addons.avatars.AvatarsAddon
import io.peekandpoke.kraft.addons.avatars.avatars
import io.peekandpoke.kraft.addons.registry.AddonRegistry.Companion.addons
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.examples.jsaddons.helpers.HorizontalContentAndCode
import io.peekandpoke.kraft.semanticui.forms.UiInputField
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag
import kotlinx.html.img
import kotlinx.html.p

@Suppress("FunctionName")
fun Tag.MinIdenticonsExample() = comp {
    MinIdenticonsExample(it)
}

class MinIdenticonsExample(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    // <CodeBlock subscribing>
    private val avatarsAddon: AvatarsAddon? by subscribingTo(addons.avatars)
    // </CodeBlock>

    private var name by value("alienHead66")
    private var saturation by value(50)
    private var lightness by value(50)

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        val addon = avatarsAddon

        ui.segment {
            ui.header H2 { +"Avatars (via AddonRegistry)" }

            if (addon == null) {
                ui.placeholder.segment {
                    ui.icon.header {
                        icon.spinner_loading()
                        +"Loading avatars addon..."
                    }
                }
                return@segment
            }

            ui.dividing.header { +"Subscribing to the addon" }

            HorizontalContentAndCode(
                ExtractedCodeBlocks.avatars_MinIdenticonsExample_kt_subscribing,
            ) {
                ui.label { +"AvatarsAddon is loaded via subscribingTo(addons.avatars)" }
            }

            ui.dividing.header { +"MinIdenticon Examples" }

            HorizontalContentAndCode(
                ExtractedCodeBlocks.avatars_MinIdenticonsExample_kt_usage,
            ) {
                // <CodeBlock usage>
                ui.three.doubling.cards {
                    ui.card {
                        noui.image {
                            img {
                                src = addon.getDataUrl("alienHead66")
                            }
                        }
                        noui.content {
                            p { +"alienHead66" }
                        }
                    }
                    ui.card {
                        noui.image {
                            img {
                                src = addon.getDataUrl(
                                    name = name,
                                    saturation = saturation,
                                    lightness = lightness,
                                )
                            }
                        }
                        noui.content {
                            ui.form {
                                UiInputField(::name) {
                                    label("Name")
                                }
                                UiInputField(::saturation) {
                                    label("Saturation")
                                }
                                UiInputField(::lightness) {
                                    label("Lightness")
                                }
                            }
                        }
                    }
                    ui.card {
                        noui.image {
                            img {
                                src = addon.getRandomDataUrl()
                            }
                        }
                        noui.content {
                            +"Random"
                        }
                    }
                }
                // </CodeBlock>
            }
        }
    }
}
