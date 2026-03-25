package io.peekandpoke.kraft.examples.fomanticui.pages.howto.blocks.dnd

import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.routing.PageTitle
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag
import kotlinx.html.p
import kotlin.math.floor

@Suppress("FunctionName")
fun Tag.DragAndDropPage() = comp {
    DragAndDropPage(it)
}

class DragAndDropPage(ctx: NoProps) : PureComponent(ctx) {

    private fun Number.isInteger(): Boolean = floor(this.toDouble()) == this

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
                        it.isInteger() && it.toInt() % 2 == 0
                    }
                    DropZoneComponent("Accepts 3") {
                        it == 3
                    }
                    DropZoneComponent("Accepts greater 4") {
                        it.toDouble() > 4
                    }
                    DropZoneComponent("Accepts all Ints") {
                        it.isInteger()
                    }
                    DropZoneComponent("Accepts all Floating points") {
                        !it.isInteger()
                    }
                }
            }
        }
    }
}
