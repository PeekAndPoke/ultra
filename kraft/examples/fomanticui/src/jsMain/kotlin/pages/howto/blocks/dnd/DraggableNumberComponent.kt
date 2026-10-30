package io.peekandpoke.kraft.examples.fomanticui.pages.howto.blocks.dnd

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.dnd.DndManager.Companion.dnd
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onPointerDown
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag
import kotlinx.html.span
import kotlinx.html.style

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
            // The grip drags; the rest of the tile scrolls a list on touch as usual
            span {
                // For touch: no scrolling from here, and a long press must not select text or open the callout
                style = "touch-action: none; user-select: none; -webkit-touch-callout: none; cursor: grab;"

                onPointerDown { event ->
                    dnd.startDrag<Number>(
                        event = event,
                        payload = props.value,
                        preview = { ui.compact.inverted.segment { +"${props.value}" } },
                    )
                }

                icon.grip_vertical()
            }

            +"Value: ${props.value} (${props.value::class.simpleName})"
        }
    }
}
