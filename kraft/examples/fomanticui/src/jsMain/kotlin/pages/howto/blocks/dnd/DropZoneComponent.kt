package de.peekandpoke.kraft.examples.fomanticui.pages.howto.blocks.dnd

import de.peekandpoke.kraft.addons.dnd.DndDropTarget
import de.peekandpoke.kraft.addons.dnd.greenHighlights
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag
import kotlinx.html.div

@Suppress("FunctionName")
fun Tag.DropZoneComponent(
    label: String,
    accepts: (Number) -> Boolean,
) = comp(
    DropZoneComponent.Props(
        label = label,
        accepts = accepts,
    )
) { DropZoneComponent(it) }

class DropZoneComponent(ctx: Ctx<Props>) : Component<DropZoneComponent.Props>(ctx) {

    data class Props(
        val label: String,
        val accepts: (Number) -> Boolean,
    )

    var counter by value(0.0)

    override fun VDom.render() {

        ui.segment {
            DndDropTarget<Number> {
                accepts = props.accepts
                onDrop = { counter += it.toDouble() }
                greenHighlights()
            }

            ui.label {
                +props.label
            }

            div {
                +"Counter: $counter"
            }
        }
    }
}
