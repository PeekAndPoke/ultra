package de.peekandpoke.kraft.examples.fomanticui.pages.howto.blocks.flashmessages

import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.examples.fomanticui.kraft
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.FlashMessagesPage() = comp {
    FlashMessagesPage(it)
}

class FlashMessagesPage(ctx: NoProps) : PureComponent(ctx) {

    override fun VDom.render() {
        PageTitle("How To | Flash Messages")

        ui.basic.padded.segment {
            ui.header H2 { +"Flash Messages" }

            ui.four.column.grid {
                noui.column {
                    ui.info.button {
                        onClick {
                            kraft.flash.info("This is an info message")
                        }

                        +"Click Me"
                    }
                }

                noui.column {
                    ui.warning.button {
                        onClick {
                            kraft.flash.warning("This is a warning message")
                        }

                        +"Click Me"
                    }
                }

                noui.column {
                    ui.error.button {
                        onClick {
                            kraft.flash.error("This is an error message")
                        }

                        +"Click Me"
                    }
                }
            }
        }
    }
}
