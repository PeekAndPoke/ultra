package io.peekandpoke.kraft.examples.fomanticui.pages.howto.blocks.dnd

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.semanticui.dnd.DndDragHandle
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.ui
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
