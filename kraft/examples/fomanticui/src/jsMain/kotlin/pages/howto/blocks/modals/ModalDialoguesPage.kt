package io.peekandpoke.kraft.examples.fomanticui.pages.howto.blocks.modals

import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.modals.ModalsManager.Companion.modals
import io.peekandpoke.kraft.routing.PageTitle
import io.peekandpoke.kraft.semanticui.modals.OkCancelModal.Companion.mini
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.ui
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
                    modals.show { handle ->
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
