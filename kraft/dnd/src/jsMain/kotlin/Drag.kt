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
    /** Where the drag came from, as the starter said; null if it said nothing. */
    val source: Any?,
    /** The pointer's position in viewport coordinates (`clientX`, `clientY`). */
    val position: Vector2D,
    /** "mouse", "pen" or "touch". */
    val pointerType: String,
)

/** What the starter of a drag learns about its end. A press that never moved past the threshold is a click: neither is called. */
interface DragHandle {
    /** Called once, when the drag was dropped on a zone, given to the callback. */
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
    /** The payload type the zone takes, matched by class (generic arguments are erased: give a payload a class of its own). */
    internal val type: KClass<T>,
    /** The element whose area is the zone; read at every hit test, so it may change. Null: the zone is not hit. */
    val element: () -> Element?,
    private val accepts: (Drag<T>) -> Boolean,
    private val handlers: Handlers<T>,
) {
    /**
     * The zone's event handlers, set in the builder of `dropZone { }`. Handlers stack, as lifecycle hooks do: each call
     * adds one, so a helper's handlers and the component's own all run, in the order they were added.
     */
    class Handlers<T : Any> {
        internal val dragStart = mutableListOf<(Drag<T>) -> Unit>()
        internal val dragStop = mutableListOf<(Drag<T>) -> Unit>()
        internal val enter = mutableListOf<(Drag<T>) -> Unit>()
        internal val over = mutableListOf<(Drag<T>) -> Unit>()
        internal val leave = mutableListOf<(Drag<T>) -> Unit>()
        internal val drop = mutableListOf<(Drag<T>) -> Unit>()

        /** A drag this zone accepts has started, anywhere: e.g. highlight that it can take it. */
        fun onDragStart(handler: (Drag<T>) -> Unit) {
            dragStart.add(handler)
        }

        /** A drag that [onDragStart] announced has ended, dropped here, elsewhere or cancelled. */
        fun onDragStop(handler: (Drag<T>) -> Unit) {
            dragStop.add(handler)
        }

        /** The pointer of an accepted drag came over this zone. */
        fun onEnter(handler: (Drag<T>) -> Unit) {
            enter.add(handler)
        }

        /** The pointer moved within this zone. */
        fun onOver(handler: (Drag<T>) -> Unit) {
            over.add(handler)
        }

        /** The pointer left this zone, or the drag ended over it without a drop here (Esc). */
        fun onLeave(handler: (Drag<T>) -> Unit) {
            leave.add(handler)
        }

        /** The drag was dropped on this zone. */
        fun onDrop(handler: (Drag<T>) -> Unit) {
            drop.add(handler)
        }
    }

    /** True when [drag] is of the zone's type and its predicate takes it; a predicate that throws takes nothing. */
    internal fun takes(drag: Drag<*>): Boolean = typed(drag)?.let { typedDrag -> guardedOr(false) { accepts(typedDrag) } } ?: false

    /** The zone's element now; one whose lookup throws is not hit. */
    internal fun area(): Element? = guardedOr(null) { element() }

    internal fun dragStart(drag: Drag<*>) = notify(handlers.dragStart, drag)
    internal fun dragStop(drag: Drag<*>) = notify(handlers.dragStop, drag)
    internal fun enter(drag: Drag<*>) = notify(handlers.enter, drag)
    internal fun over(drag: Drag<*>) = notify(handlers.over, drag)
    internal fun leave(drag: Drag<*>) = notify(handlers.leave, drag)
    internal fun drop(drag: Drag<*>) = notify(handlers.drop, drag)

    private fun notify(list: List<(Drag<T>) -> Unit>, drag: Drag<*>) {
        typed(drag)?.let { typedDrag -> list.toList().forEach { handler -> guarded { handler(typedDrag) } } }
    }

    @Suppress("UNCHECKED_CAST")
    private fun typed(drag: Drag<*>): Drag<T>? = if (type.isInstance(drag.payload)) drag as Drag<T> else null
}

/** Runs a handler of the app's: one that throws is reported and does not keep the drag from ending cleanly. */
internal fun guarded(handler: () -> Unit) = guardedOr(Unit, handler)

/** Runs [block], code of the app's; one that throws is reported and gives [fallback]. */
internal fun <R> guardedOr(fallback: R, block: () -> R): R = try {
    block()
} catch (e: Throwable) {
    console.error("[kraft:dnd] a handler failed", e)
    fallback
}
