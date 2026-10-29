package io.peekandpoke.kraft.dnd

import io.peekandpoke.kraft.utils.Vector2D
import kotlinx.html.FlowContent
import org.w3c.dom.Element
import kotlin.reflect.KClass

/** The preview the [DndStage] draws at the pointer while a drag runs. */
typealias DragPreview = FlowContent.() -> Unit

/**
 * A drag in progress (ULTRA-00265): what is dragged, from where, and where the pointer is.
 *
 * [payload] is what a drop zone receives, [source] whatever the starter gave to say where it came from (a slot, a
 * list and an index), so a zone can tell a swap from a move.
 */
class Drag<out T : Any> internal constructor(
    /** What is dragged. */
    val payload: T,
    /** The payload's type, which drop zones filter by. */
    val type: KClass<out T>,
    /** Where the drag came from, as the starter said; null if it said nothing. */
    val source: Any?,
    /** The pointer's position in viewport coordinates (`clientX`, `clientY`). */
    val position: Vector2D,
    /** "mouse", "pen" or "touch". */
    val pointerType: String,
)

/** What the starter of a drag learns about its end. A press that never moved past the threshold is a click: neither is called. */
interface DragHandle {
    /** Called once, when the drag was dropped on [zone]. */
    fun onDropped(callback: (zone: DropZone<*>) -> Unit): DragHandle

    /** Called once, when the drag ended without a drop: released outside every zone, Esc, or cancelled. */
    fun onCancelled(callback: () -> Unit): DragHandle
}

/**
 * A place a drag can be dropped on: registered with [DndManager.register], usually by `Component.dropZone`.
 *
 * Every event goes to the zone's own handlers; the zone keeps its own UI state (highlighted, hovered) and renders
 * from it. The manager never writes to the zone's DOM.
 */
class DropZone<T : Any> internal constructor(
    /** The payload type the zone takes. */
    val type: KClass<T>,
    /** The element whose area is the zone; read at every hit test, so it may change. Null: the zone is not hit. */
    val element: () -> Element?,
    private val accepts: (Drag<T>) -> Boolean,
    private val handlers: Handlers<T>,
) {
    /** The zone's event handlers, set in the builder of `dropZone { }`. */
    class Handlers<T : Any> {
        internal var dragStart: (Drag<T>) -> Unit = {}
        internal var dragStop: (Drag<T>) -> Unit = {}
        internal var enter: (Drag<T>) -> Unit = {}
        internal var over: (Drag<T>) -> Unit = {}
        internal var leave: (Drag<T>) -> Unit = {}
        internal var drop: (Drag<T>) -> Unit = {}

        /** A drag this zone accepts has started, anywhere: e.g. highlight that it can take it. */
        fun onDragStart(handler: (Drag<T>) -> Unit) {
            dragStart = handler
        }

        /** A drag that [onDragStart] announced has ended, dropped here, elsewhere or cancelled. */
        fun onDragStop(handler: (Drag<T>) -> Unit) {
            dragStop = handler
        }

        /** The pointer of an accepted drag came over this zone. */
        fun onEnter(handler: (Drag<T>) -> Unit) {
            enter = handler
        }

        /** The pointer moved within this zone. */
        fun onOver(handler: (Drag<T>) -> Unit) {
            over = handler
        }

        /** The pointer left this zone, or the drag ended over it without a drop here (Esc). */
        fun onLeave(handler: (Drag<T>) -> Unit) {
            leave = handler
        }

        /** The drag was dropped on this zone. */
        fun onDrop(handler: (Drag<T>) -> Unit) {
            drop = handler
        }
    }

    /** True when [drag] is of the zone's type and its predicate takes it. */
    internal fun takes(drag: Drag<*>): Boolean = typed(drag)?.let(accepts) ?: false

    internal fun dragStart(drag: Drag<*>) = typed(drag)?.let(handlers.dragStart)
    internal fun dragStop(drag: Drag<*>) = typed(drag)?.let(handlers.dragStop)
    internal fun enter(drag: Drag<*>) = typed(drag)?.let(handlers.enter)
    internal fun over(drag: Drag<*>) = typed(drag)?.let(handlers.over)
    internal fun leave(drag: Drag<*>) = typed(drag)?.let(handlers.leave)
    internal fun drop(drag: Drag<*>) = typed(drag)?.let(handlers.drop)

    @Suppress("UNCHECKED_CAST")
    private fun typed(drag: Drag<*>): Drag<T>? = if (type.isInstance(drag.payload)) drag as Drag<T> else null
}
