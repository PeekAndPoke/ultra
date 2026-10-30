package io.peekandpoke.kraft.dnd

import io.peekandpoke.kraft.KraftApp
import io.peekandpoke.kraft.utils.Vector2D
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSource
import io.peekandpoke.ultra.streams.Unsubscribe
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.FlowContent
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.pointerevents.PointerEvent
import kotlin.math.hypot

/** The [DndManager] that `kraftApp { dnd() }` registers; disposing the app ends a drag in progress or pending. */
class DefaultDndManager : DndManager, KraftApp.Disposable {

    /**
     * What the stage draws: the preview's renderer at the pointer ([left], [top]), shifted by [grabX] / [grabY] of its
     * own size, so the spot of the source the pointer grabbed (as a share of its size) sits under the pointer whatever
     * the preview's size.
     */
    internal class Preview(
        val render: DragPreview,
        val left: Double,
        val top: Double,
        val grabX: Double,
        val grabY: Double,
    )

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
        source: Any?,
        preview: DragPreview?,
        threshold: Double,
    ): DragHandle {
        // The primary button only: a right press opens a context menu, which takes the release
        if (!event.isPrimary || event.button.toInt() != 0) return NoDrag

        // A press in a text field selects text; it does not drag (isContentEditable minds a contenteditable="false" island)
        val target = event.target as? Element
        if (target?.closest(FORM_FIELDS) != null || (target as? HTMLElement)?.isContentEditable == true) return NoDrag

        // A nested source: the first start for this event wins, the innermost one as the event bubbles
        if (session?.startedBy === event) return NoDrag

        // One drag at a time; one of another pointer is still down, and its release's click must not land
        session?.let { it.end(drop = false, ending = if (it.pointerId == event.pointerId) Ending.PointerGone else Ending.PointerStillDown) }

        return Session(event = event, payload = payload, source = source, render = preview, threshold = threshold)
            .also { session = it }
    }

    override fun cancel() {
        // From a handler of the release itself, the click follows at once
        session?.let { it.end(drop = false, ending = if (it.releasing) Ending.Released else Ending.PointerStillDown) }
    }

    override fun dispose() {
        // A pending press too: its window listeners would otherwise outlive the app; and a release still waited for
        session?.end(drop = false, ending = Ending.PointerGone)
        releaseWaiter?.invoke()
        clickSwallower?.invoke()
    }

    /** Stops waiting for the release after an Esc or [cancel], while one is waited for. */
    private var releaseWaiter: (() -> Unit)? = null

    /** Stops waiting for the click after a release, while one is waited for (a tap's comes late). */
    private var clickSwallower: (() -> Unit)? = null

    /** How a drag ends, for the click the browser may fire after the release. */
    private enum class Ending {
        /** Released here: the click follows at once. */
        Released,

        /** Ended while the pointer is still down (Esc, [cancel]): the click follows its release. */
        PointerStillDown,

        /** No click follows (pointercancel, no button held any more, a new press of the same pointer, dispose). */
        PointerGone,
    }

    /** The handle of a press that does not drag. */
    private object NoDrag : DragHandle {
        override fun onDropped(callback: (zone: DropZone<*>) -> Unit): DragHandle = this
        override fun onCancelled(callback: () -> Unit): DragHandle = this
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
        private val source: Any?,
        private val render: DragPreview?,
        private val threshold: Double,
    ) : DragHandle {

        /** The pointerdown that started it. */
        val startedBy: Event = event

        /** The release is being handled: a cancel from one of its handlers ends a released drag. */
        var releasing = false
            private set

        val pointerId = event.pointerId
        private val pointerType = event.pointerType
        private val startAt = Vector2D(event.clientX.toDouble(), event.clientY.toDouble())
        private val captured: Element? = event.target as? Element

        /**
         * Where the pointer grabbed the element, as a share of its width and height (0..1): the preview may be smaller
         * than the source (a tile for a whole row), and the same share of it goes under the pointer.
         */
        private val grab: Vector2D = (event.currentTarget as? Element ?: captured)
            ?.getBoundingClientRect()
            ?.takeIf { it.width > 0.0 && it.height > 0.0 }
            ?.let {
                Vector2D(
                    ((startAt.x - it.left) / it.width).coerceIn(0.0, 1.0),
                    ((startAt.y - it.top) / it.height).coerceIn(0.0, 1.0),
                )
            }
            ?: Vector2D.zero

        /** The page's user-select (and its -webkit- twin) before the drag took it over. */
        private var userSelectBefore: String? = null
        private var webkitUserSelectBefore: String? = null

        private var drag = Drag(payload = payload, source = source, position = startAt, pointerType = pointerType)

        private var started = false
        private var ended = false

        /** Zones told of the start, which hear the stop. */
        private val told = mutableListOf<DropZone<*>>()
        private var hovered: DropZone<*>? = null

        private val droppedCallbacks = mutableListOf<(DropZone<*>) -> Unit>()
        private val cancelledCallbacks = mutableListOf<() -> Unit>()

        private val onMove: (Event) -> Unit = { ours(it)?.let(::moved) }
        private val onUp: (Event) -> Unit = { ours(it)?.let(::released) }
        private val onCancel: (Event) -> Unit = { ours(it)?.let { end(drop = false, ending = Ending.PointerGone) } }
        private val onKey: (Event) -> Unit = { evt ->
            if (started && (evt as? KeyboardEvent)?.key == "Escape") {
                evt.preventDefault()
                evt.stopPropagation()
                end(drop = false, ending = Ending.PointerStillDown)
            }
        }

        // The browser's own drag (an image, a link, selected text) would cancel ours with a pointercancel, and so would a
        // long press's context menu (Android) on an image or a link
        private val onNativeDrag: (Event) -> Unit = { it.preventDefault() }

        private fun ours(event: Event): PointerEvent? = (event as? PointerEvent)?.takeIf { it.pointerId == pointerId }

        // A press while this drag runs means its release was lost: a press of this pointer, or (touch) a new primary
        // touch, which only a finger on an empty screen is
        private val onPress: (Event) -> Unit = { evt ->
            val press = evt as? PointerEvent
            val lost = press != null && (
                press.pointerId == pointerId ||
                    (pointerType == "touch" && press.pointerType == "touch" && press.isPrimary)
                )

            if (lost) end(drop = false, ending = Ending.PointerGone)
        }

        init {
            // Listened to on the window in the capture phase: the events reach it wherever the pointer is
            window.addEventListener("pointermove", onMove, true)
            window.addEventListener("pointerup", onUp, true)
            window.addEventListener("pointercancel", onCancel, true)
            window.addEventListener("keydown", onKey, true)
            window.addEventListener("dragstart", onNativeDrag, true)
            window.addEventListener("contextmenu", onNativeDrag, true)
            window.addEventListener("pointerdown", onPress, true)

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
            if (event.pointerType == "mouse" || event.pointerType == "pen") {
                val buttons = event.buttons.toInt()

                // No button held: the release was lost (a context menu, a window switch)
                if (buttons == 0) {
                    end(drop = false, ending = Ending.PointerGone)
                    return
                }

                // The primary button let go while another is held (a chord: no pointerup comes): that is the release
                if (buttons and 1 == 0) {
                    released(event)
                    return
                }
            }

            val at = at(event)

            if (!started) {
                if (hypot(at.x - startAt.x, at.y - startAt.y) < threshold) return
                start()
            }

            // A handler may have ended the drag (dnd.cancel()), after any of these steps
            if (ended) return
            currentSource(drag)
            if (ended) return
            render?.let { preview(Preview(render = it, left = at.x, top = at.y, grabX = grab.x, grabY = grab.y)) }

            hitTest(at)
        }

        private fun released(event: PointerEvent) {
            releasing = true

            try {
                // The layout may have moved under a still pointer (a wheel scroll, a re-render): the drop goes where
                // it is now
                if (started) hitTest(at(event))
            } finally {
                end(drop = true, ending = Ending.Released)
            }
        }

        private fun at(event: PointerEvent): Vector2D =
            Vector2D(event.clientX.toDouble(), event.clientY.toDouble())
                .also { drag = Drag(payload = payload, source = source, position = it, pointerType = pointerType) }

        private fun start() {
            started = true

            // No text selection while the pointer drags across the page
            val style = document.documentElement?.asDynamic()?.style
            userSelectBefore = style?.getPropertyValue("user-select")?.unsafeCast<String>()
            webkitUserSelectBefore = style?.getPropertyValue("-webkit-user-select")?.unsafeCast<String>()
            style?.setProperty("user-select", "none")
            style?.setProperty("-webkit-user-select", "none")
            window.asDynamic().getSelection()?.removeAllRanges()

            zones.toList().filter { it.takes(drag) }.forEach { zone ->
                if (!ended) {
                    told.add(zone)
                    zone.dragStart(drag)
                }
            }
        }

        /** The innermost accepting zone under [at] gets enter / over; the one it replaces gets leave. */
        private fun hitTest(at: Vector2D) {
            // The topmost element there that is not the stage's: a preview part with pointer-events of its own is no zone
            val hit: Node? = document.elementsFromPoint(at.x, at.y).firstOrNull { it.closest(".kraft-dnd-stage") == null }

            // Only zones told of the start: they are the ones that hear the stop
            val next = hit?.let { node ->
                told.toList()
                    .filter { it.takes(drag) }
                    .mapNotNull { zone -> zone.area()?.takeIf { it.contains(node) }?.let { zone to it } }
                    // Innermost: the zone whose element lies inside all the others'
                    .maxByOrNull { (_, element) -> element.depth() }
                    ?.first
            }

            if (next === hovered) {
                next?.over(drag)
                return
            }

            // Left before its handler runs: a cancel from it must not make the zone hear leave twice
            val leaving = hovered
            hovered = null
            leaving?.leave(drag)
            if (ended) return
            hovered = next
            next?.enter(drag)
            if (ended) return
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
        fun end(drop: Boolean, ending: Ending) {
            if (ended) return
            ended = true

            window.removeEventListener("pointermove", onMove, true)
            window.removeEventListener("pointerup", onUp, true)
            window.removeEventListener("pointercancel", onCancel, true)
            window.removeEventListener("keydown", onKey, true)
            window.removeEventListener("dragstart", onNativeDrag, true)
            window.removeEventListener("contextmenu", onNativeDrag, true)
            window.removeEventListener("pointerdown", onPress, true)

            try {
                captured?.releasePointerCapture(pointerId)
            } catch (_: Throwable) {
            }

            if (session === this) session = null

            // A press that never moved past the threshold was a click, not a drag
            if (!started) return

            preview(null)
            currentSource(null)

            val style = document.documentElement?.asDynamic()?.style
            restore(style, "user-select", userSelectBefore)
            restore(style, "-webkit-user-select", webkitUserSelectBefore)

            val target = hovered?.takeIf { drop }

            // The app's handlers are guarded: one that throws does not keep the others, and the stops, from running
            if (target != null) {
                target.drop(drag)
                droppedCallbacks.forEach { guarded { it(target) } }
            } else {
                hovered?.leave(drag)
                cancelledCallbacks.forEach { guarded { it() } }
            }

            hovered = null
            told.toList().forEach { it.dragStop(drag) }
            told.clear()

            // After the handlers: a drop handler's own element.click() (a file picker) is not taken for the release's
            when (ending) {
                Ending.Released -> swallowNextClick(pointerType)
                Ending.PointerStillDown -> swallowClickAfterRelease(pointerId, pointerType)
                Ending.PointerGone -> Unit
            }
        }

        private fun restore(style: dynamic, property: String, before: String?) {
            if (before.isNullOrEmpty()) style?.removeProperty(property) else style?.setProperty(property, before)
        }
    }

    /**
     * The click a browser fires after the pointer goes up ends the drag; it must not reach the dragged element. A mouse's
     * click follows in the same task; a tap's comes later (the browser's gesture recognition), so touch and pen wait a
     * little longer.
     */
    private fun swallowNextClick(pointerType: String) {
        clickSwallower?.invoke()

        lateinit var swallow: (Event) -> Unit
        lateinit var onPress: (Event) -> Unit
        lateinit var stop: () -> Unit

        stop = {
            window.removeEventListener("click", swallow, true)
            window.removeEventListener("pointerdown", onPress, true)
            if (clickSwallower === stop) clickSwallower = null
        }

        swallow = { event ->
            event.stopPropagation()
            event.preventDefault()
            stop()
        }

        // A new press: the release's click did not come (a moved touch is no tap), and the next click is the user's
        onPress = { stop() }

        window.addEventListener("click", swallow, true)
        window.addEventListener("pointerdown", onPress, true)
        clickSwallower = stop
        // No click came (released elsewhere): stop waiting
        window.setTimeout({ stop() }, if (pointerType == "mouse") 0 else TAP_CLICK_WAIT_MS)
    }

    /**
     * A drag ended with the pointer still down: the click after its release must not reach the element either. Waited
     * for until that release, a new press of the pointer (the release was lost) or dispose.
     */
    private fun swallowClickAfterRelease(pointerId: Int, pointerType: String) {
        releaseWaiter?.invoke()

        lateinit var onRelease: (Event) -> Unit
        lateinit var stop: () -> Unit

        stop = {
            window.removeEventListener("pointerup", onRelease, true)
            window.removeEventListener("pointercancel", onRelease, true)
            window.removeEventListener("pointerdown", onRelease, true)
            if (releaseWaiter === stop) releaseWaiter = null
        }

        onRelease = { event ->
            if ((event as? PointerEvent)?.pointerId == pointerId) {
                stop()

                if (event.type == "pointerup") swallowNextClick(pointerType)
            }
        }

        window.addEventListener("pointerup", onRelease, true)
        window.addEventListener("pointercancel", onRelease, true)
        window.addEventListener("pointerdown", onRelease, true)
        releaseWaiter = stop
    }

    private companion object {
        /** Form fields, where a press selects text or edits, and does not drag. */
        const val FORM_FIELDS = "input, textarea, select"

        /** How long the click of a touch or pen tap may come after its release. */
        const val TAP_CLICK_WAIT_MS = 400
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
