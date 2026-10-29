package io.peekandpoke.kraft.semanticui.dnd

import io.peekandpoke.kraft.dnd.DropZone

/**
 * The UI state of a drop zone, for a component to render from (kraft:dnd leaves the zone's look to the component):
 * [canTake] while a drag the zone accepts runs, [hovered] while that drag is over it.
 *
 * ```
 * private val zone = DropZoneState()
 *
 * init {
 *     dropZone<Look> {
 *         track(zone) { triggerRedraw() }
 *         onDrop { equip(it.payload) }
 *     }
 * }
 *
 * override fun VDom.render() {
 *     ui.with(*zone.semanticClasses).segment { ... }
 * }
 * ```
 */
class DropZoneState {
    /** A drag this zone accepts is running. */
    var canTake: Boolean = false
        internal set

    /** That drag is over this zone. */
    var hovered: Boolean = false
        internal set

    /** Fomantic UI classes: `green` while hovered, `secondary` while the zone can take the drag, none otherwise. */
    val semanticClasses: Array<String>
        get() = when {
            hovered -> arrayOf("green")
            canTake -> arrayOf("secondary")
            else -> emptyArray()
        }
}

/** Keeps [state] in step with this zone's events, calling [redraw] after each change. */
fun <T : Any> DropZone.Handlers<T>.track(state: DropZoneState, redraw: () -> Unit) {
    onDragStart {
        state.canTake = true
        redraw()
    }
    onDragStop {
        state.canTake = false
        state.hovered = false
        redraw()
    }
    onEnter {
        state.hovered = true
        redraw()
    }
    onLeave {
        state.hovered = false
        redraw()
    }
}
