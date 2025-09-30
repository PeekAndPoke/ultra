package de.peekandpoke.kraft.examples.fomanticui.pages.howto.blocks.dnd

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.semanticui.dnd.DndDragHandle
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.semanticui.icon
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.DraggableNumberComponent(
    value: Number,
) = comp(
    DraggableNumberComponent.Props(
        value = value,
    )
) { DraggableNumberComponent(it) }

class DraggableNumberComponent(ctx: Ctx<Props>) : Component<DraggableNumberComponent.Props>(ctx) {

    data class Props(
        val value: Number,
    )

    override fun VDom.render() {
        ui.segment {

            icon.grip_vertical.link {
                DndDragHandle(payload = props.value)
            }

            +"Value: ${props.value} (${props.value::class.simpleName})"
        }
    }
}
