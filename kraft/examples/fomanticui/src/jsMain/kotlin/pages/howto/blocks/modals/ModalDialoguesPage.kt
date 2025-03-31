package de.peekandpoke.kraft.examples.fomanticui.pages.howto.blocks.modals

import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.addons.semanticui.modals.OkCancelModal.Companion.mini
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.examples.fomanticui.kraft
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.browser.window
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.ModalDialogsPage() = comp {
    ModalDialoguesPage(it)
}

class ModalDialoguesPage(ctx: NoProps) : PureComponent(ctx) {

    override fun VDom.render() {
        PageTitle("How To | Modal Dialogues")

        ui.basic.padded.segment {
            ui.header H2 { +"Modal Dialogues" }

            ui.button {
                onClick {
                    kraft.modals.show { handle ->
                        mini(
                            handle = handle,
                            header = { ui.header { +"This is a mini modal" } },
                            content = { ui.content { +"It has a content" } },
                        ) { result ->
                            window.alert("mini modal closed with result: $result")
                        }
                    }
                }

                +"Click me!"
            }
        }
    }
}
