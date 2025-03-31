package de.peekandpoke.kraft.examples.jsaddons.avatars

import de.peekandpoke.kraft.addons.avatars.Avatars
import de.peekandpoke.kraft.addons.semanticui.forms.UiInputField
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
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
