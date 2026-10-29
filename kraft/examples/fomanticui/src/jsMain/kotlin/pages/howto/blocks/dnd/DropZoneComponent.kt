package io.peekandpoke.kraft.examples.fomanticui.pages.howto.blocks.dnd

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.dnd.dropZone
import io.peekandpoke.kraft.semanticui.dnd.DropZoneState
import io.peekandpoke.kraft.semanticui.dnd.track
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.ui
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

    private var counter by value(0.0)

    private val zone = DropZoneState()

    init {
        dropZone<Number>(accepts = { props.accepts(it.payload) }) {
            track(zone) { triggerRedraw() }
            onDrop { counter += it.payload.toDouble() }
        }
    }

    override fun VDom.render() {
        ui.with(*zone.semanticClasses).segment {
            ui.label {
                +props.label
            }

            div {
                +"Counter: $counter"
            }
        }
    }
}
