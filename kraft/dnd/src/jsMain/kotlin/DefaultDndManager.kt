package io.peekandpoke.kraft.dnd

import io.peekandpoke.kraft.utils.Vector2D
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSource
import io.peekandpoke.ultra.streams.Unsubscribe
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.FlowContent
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.pointerevents.PointerEvent
import kotlin.math.hypot
import kotlin.reflect.KClass

/** The [DndManager] that `kraftApp { dnd() }` registers. */
class DefaultDndManager : DndManager {

    /** What the stage draws: the preview's renderer at its place in the viewport. */
    internal class Preview(val render: DragPreview, val left: Double, val top: Double)

    private val currentSource = StreamSource<Drag<*>?>(null)

    override val current: Stream<Drag<*>?> = currentSource.readonly

    /** For the [DndStage]: the preview of the drag in progress. */
    internal val preview = StreamSource<Preview?>(null)

    private val zones = mutableListOf<DropZone<*>>()

    private var session: Session<*>? = null

    override fun autoMount(element: FlowContent) {
        with(element) {
            DndStage(manager = this@DefaultDndManager)
        }
    }

    override fun <T : Any> startDrag(
        event: PointerEvent,
        payload: T,
        type: KClass<T>,
        source: Any?,
        preview: DragPreview?,
        threshold: Double,
    ): DragHandle {
        // One drag at a time
        session?.end(drop = false)

        return Session(event = event, payload = payload, type = type, source = source, render = preview, threshold = threshold)
            .also { session = it }
    }

    override fun cancel() {
        session?.end(drop = false)
    }

    override fun register(zone: DropZone<*>): Unsubscribe {
        zones.add(zone)
        session?.zoneAdded(zone)

        return {
            zones.remove(zone)
            session?.zoneRemoved(zone)
        }
    }

    /** One drag, from the pointer going down to its end. */
    private inner class Session<T : Any>(
        event: PointerEvent,
        private val payload: T,
        private val type: KClass<T>,
        private val source: Any?,
        private val render: DragPreview?,
        private val threshold: Double,
    ) : DragHandle {

        private val pointerId = event.pointerId
        private val pointerType = event.pointerType
        private val startAt = Vector2D(event.clientX.toDouble(), event.clientY.toDouble())
        private val captured: Element? = event.target as? Element

        /** Where the pointer grabbed the element, so the preview keeps that spot under the pointer. */
        private val grab: Vector2D = (event.currentTarget as? Element ?: captured)
            ?.getBoundingClientRect()
            ?.let { Vector2D(startAt.x - it.left, startAt.y - it.top) }
            ?: Vector2D.zero

        private var drag = Drag(payload = payload, type = type, source = source, position = startAt, pointerType = pointerType)

        private var started = false
        private var ended = false

        /** Zones told of the start, which hear the stop. */
        private val told = mutableListOf<DropZone<*>>()
        private var hovered: DropZone<*>? = null

        private val droppedCallbacks = mutableListOf<(DropZone<*>) -> Unit>()
        private val cancelledCallbacks = mutableListOf<() -> Unit>()

        private val onMove: (Event) -> Unit = { (it as? PointerEvent)?.takeIf { e -> e.pointerId == pointerId }?.let(::moved) }
        private val onUp: (Event) -> Unit = { (it as? PointerEvent)?.takeIf { e -> e.pointerId == pointerId }?.let { end(drop = true) } }
        private val onCancel: (Event) -> Unit = { (it as? PointerEvent)?.takeIf { e -> e.pointerId == pointerId }?.let { end(drop = false) } }
        private val onKey: (Event) -> Unit = { evt ->
            if (started && (evt as? KeyboardEvent)?.key == "Escape") {
                evt.preventDefault()
                evt.stopPropagation()
                end(drop = false)
            }
        }

        init {
            // Listened to on the window in the capture phase: the events reach it wherever the pointer is
            window.addEventListener("pointermove", onMove, true)
            window.addEventListener("pointerup", onUp, true)
            window.addEventListener("pointercancel", onCancel, true)
            window.addEventListener("keydown", onKey, true)

            // Best effort: a synthetic or already released pointer cannot be captured, and the window still hears it
            try {
                captured?.setPointerCapture(pointerId)
            } catch (_: Throwable) {
            }
        }

        override fun onDropped(callback: (zone: DropZone<*>) -> Unit): DragHandle = apply {
            droppedCallbacks.add(callback)
        }

        override fun onCancelled(callback: () -> Unit): DragHandle = apply {
            cancelledCallbacks.add(callback)
        }

        private fun moved(event: PointerEvent) {
            val at = Vector2D(event.clientX.toDouble(), event.clientY.toDouble())
            drag = Drag(payload = payload, type = type, source = source, position = at, pointerType = pointerType)

            if (!started) {
                if (hypot(at.x - startAt.x, at.y - startAt.y) < threshold) return
                start()
            }

            currentSource(drag)
            render?.let { preview(Preview(render = it, left = at.x - grab.x, top = at.y - grab.y)) }

            hitTest(at)
        }

        private fun start() {
            started = true

            zones.toList().filter { it.takes(drag) }.forEach { zone ->
                told.add(zone)
                zone.dragStart(drag)
            }
        }

        /** The innermost accepting zone under [at] gets enter / over; the one it replaces gets leave. */
        private fun hitTest(at: Vector2D) {
            val hit: Node? = document.elementFromPoint(at.x, at.y)

            val next = hit?.let { node ->
                zones.toList()
                    .filter { it.takes(drag) }
                    .mapNotNull { zone -> zone.element()?.takeIf { it.contains(node) }?.let { zone to it } }
                    // Innermost: the zone whose element lies inside all the others'
                    .maxByOrNull { (_, element) -> element.depth() }
                    ?.first
            }

            if (next === hovered) {
                next?.over(drag)
                return
            }

            hovered?.leave(drag)
            hovered = next
            next?.enter(drag)
            next?.over(drag)
        }

        fun zoneAdded(zone: DropZone<*>) {
            if (started && !ended && zone.takes(drag)) {
                told.add(zone)
                zone.dragStart(drag)
            }
        }

        fun zoneRemoved(zone: DropZone<*>) {
            told.remove(zone)
            if (hovered === zone) hovered = null
        }

        /** Ends the drag, dropped on the hovered zone if [drop] and there is one. */
        fun end(drop: Boolean) {
            if (ended) return
            ended = true

            window.removeEventListener("pointermove", onMove, true)
            window.removeEventListener("pointerup", onUp, true)
            window.removeEventListener("pointercancel", onCancel, true)
            window.removeEventListener("keydown", onKey, true)

            try {
                captured?.releasePointerCapture(pointerId)
            } catch (_: Throwable) {
            }

            if (session === this) session = null

            // A press that never moved past the threshold was a click, not a drag
            if (!started) return

            preview(null)
            currentSource(null)
            swallowNextClick()

            val target = hovered?.takeIf { drop }

            if (target != null) {
                target.drop(drag)
                droppedCallbacks.forEach { it(target) }
            } else {
                hovered?.leave(drag)
                cancelledCallbacks.forEach { it() }
            }

            hovered = null
            told.toList().forEach { it.dragStop(drag) }
            told.clear()
        }
    }

    /** The click a browser fires after the pointer goes up ends the drag; it must not reach the dragged element. */
    private fun swallowNextClick() {
        lateinit var swallow: (Event) -> Unit

        swallow = { event ->
            event.stopPropagation()
            event.preventDefault()
            window.removeEventListener("click", swallow, true)
        }

        window.addEventListener("click", swallow, true)
        // No click came (released elsewhere, touch): stop waiting after this task
        window.setTimeout({ window.removeEventListener("click", swallow, true) }, 0)
    }
}

/** How many ancestors the element has. */
private fun Element.depth(): Int {
    var depth = 0
    var node: Node? = parentNode

    while (node != null) {
        depth++
        node = node.parentNode
    }

    return depth
}
