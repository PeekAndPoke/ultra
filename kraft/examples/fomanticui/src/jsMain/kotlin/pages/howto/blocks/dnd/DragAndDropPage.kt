package de.peekandpoke.kraft.examples.fomanticui.pages.howto.blocks.dnd

import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import js.core.JsNumber
import kotlinx.html.Tag
import kotlinx.html.p

@Suppress("FunctionName")
fun Tag.DragAndDropPage() = comp {
    DragAndDropPage(it)
}

class DragAndDropPage(ctx: NoProps) : PureComponent(ctx) {

    override fun VDom.render() {
        PageTitle("How To | Drag and drop")

        ui.basic.padded.segment {
            ui.header H2 { +"DnD" }

            p {
                +"Drag items from the left side to the right side."
            }

            ui.three.column.grid {
                ui.column {
                    DraggableNumberComponent(1)
                    DraggableNumberComponent(2)
                    DraggableNumberComponent(3)
                    DraggableNumberComponent(4)
                    DraggableNumberComponent(5)

                    DraggableNumberComponent(5.5)
                    DraggableNumberComponent(6.6f)
                }
                ui.column {
                    DropZoneComponent("Accepts 1") {
                        it == 1
                    }
                    DropZoneComponent("Accepts %2") {
                        JsNumber.isInteger(it) && it.toInt() % 2 == 0
                    }
                    DropZoneComponent("Accepts 3") {
                        it == 3
                    }
                    DropZoneComponent("Accepts greater 4") {
                        it.toDouble() > 4
                    }
                    DropZoneComponent("Accepts all Ints") {
                        JsNumber.isInteger(it)
                    }
                    DropZoneComponent("Accepts all Floating points") {
                        !JsNumber.isInteger(it)
                    }
                }
            }
        }
    }
}
