package io.peekandpoke.kraft.examples.jsaddons.avatars

import io.peekandpoke.kraft.addons.avatars.Avatars
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.semanticui.forms.UiInputField
import io.peekandpoke.kraft.vdom.VDom
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

    private var name by value("alienHead66")
    private var saturation by value(50)
    private var lightness by value(50)

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.segment {
            ui.header H2 { +"Avatars" }

            ui.header H3 { +"MinIdenticon Examples" }

            ui.three.doubling.cards {
                ui.card {
                    noui.image {
                        img {
                            src = Avatars.MinIdenticon.getDataUrl("alienHead66")
                        }
                    }
                    noui.content {
                        p { +"alienHead66" }
                    }
                }
                ui.card {
                    noui.image {
                        img {
                            src = Avatars.MinIdenticon.getDataUrl(
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
                            src = Avatars.MinIdenticon.getRandomDataUrl()
                        }
                    }
                    noui.content {
                        +"Random"
                    }
                }
            }
        }
    }
}
