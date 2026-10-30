package io.peekandpoke.kraft.dnd

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.css
import kotlinx.css.PointerEvents
import kotlinx.css.Position
import kotlinx.css.left
import kotlinx.css.pointerEvents
import kotlinx.css.position
import kotlinx.css.px
import kotlinx.css.top
import kotlinx.css.zIndex
import kotlinx.html.Tag
import kotlinx.html.div

/** Above the modals (Fomantic's dimmer is 1000) and the popups stage (10000), so a drag works inside a modal. */
internal const val DND_STAGE_Z_INDEX = 20000

/** Mounts the [DndStage] of [manager]; the app does this through [DndManager.autoMount]. */
@Suppress("FunctionName")
internal fun Tag.DndStage(manager: DefaultDndManager) = comp(DndStage.Props(manager)) { DndStage(it) }

/**
 * Draws the preview of the drag in progress at the pointer, and nothing else. The stage takes no pointer events, and
 * the hit test looks past a preview part that sets its own.
 */
internal class DndStage(ctx: Ctx<Props>) : Component<DndStage.Props>(ctx) {

    data class Props(val manager: DefaultDndManager)

    private val preview by subscribingTo(props.manager.preview)

    override fun VDom.render() {
        val current = preview ?: return

        div(classes = "kraft-dnd-stage") {
            css {
                position = Position.fixed
                left = current.left.px
                top = current.top.px
                // The grabbed share of the preview's own size goes under the pointer
                put("transform", "translate(${-current.grabX * 100}%, ${-current.grabY * 100}%)")
                // Its own width, also near the viewport's right edge, where a fixed box would shrink to the space left
                put("width", "max-content")
                zIndex = DND_STAGE_Z_INDEX
                pointerEvents = PointerEvents.none
            }

            current.render(this)
        }
    }
}
