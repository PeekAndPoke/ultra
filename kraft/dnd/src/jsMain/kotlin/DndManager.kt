package io.peekandpoke.kraft.dnd

import io.peekandpoke.kraft.KraftApp
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.getAttributeRecursive
import io.peekandpoke.kraft.dnd.DndManager.Companion.dnd
import io.peekandpoke.ultra.common.TypedKey
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.Unsubscribe
import org.w3c.dom.Element
import org.w3c.dom.pointerevents.PointerEvent
import kotlin.reflect.KClass

/**
 * Drag and drop for a Kraft app (ULTRA-00265): one drag at a time, started by a component's pointer event, dropped on
 * zones that components register. Mouse, touch and pen alike (pointer events). Its [DndStage] is mounted with the
 * app and draws only the drag preview, above the modals. The manager never writes to the zones' DOM; on the page it
 * only turns text selection off while a drag runs.
 *
 * Register it with `kraftApp { dnd() }`; `semanticUI()` does.
 */
interface DndManager : KraftApp.AutoMountedUi {

    companion object {
        val key = TypedKey<DndManager>("dnd")

        /** The app's [DndManager], from the component tree. */
        val Component<*>.dnd: DndManager get() = getAttributeRecursive(key)
    }

    /** The drag in progress, once the pointer moved past the threshold; null otherwise. */
    val current: Stream<Drag<*>?>

    /**
     * Starts a drag from a `pointerdown` [event]: it begins once the pointer moved [threshold] pixels, so a click
     * stays a click. [preview] is drawn at the pointer, where the pointer grabbed the event's element; no preview
     * draws nothing but the cursor. Only the primary button of the primary pointer drags; another press gets a handle
     * that never calls back. Nested sources: the first `startDrag` for an event wins, the innermost one as the event
     * bubbles. For touch, the element needs `touch-action: none` before the pointer goes down (and `user-select: none`,
     * `-webkit-touch-callout: none` against a long-press selection); in a list that scrolls, give those and the
     * `pointerdown` to a grip inside the tile, so a swipe on the rest still scrolls.
     */
    fun <T : Any> startDrag(
        event: PointerEvent,
        payload: T,
        source: Any? = null,
        preview: DragPreview? = null,
        threshold: Double = 4.0,
    ): DragHandle

    /** Ends the drag in progress, if any, as cancelled. */
    fun cancel()

    /** Registers [zone] until the returned [Unsubscribe] is called. A zone removed mid-drag gets no more events. */
    fun register(zone: DropZone<*>): Unsubscribe
}

/** Registers a [DefaultDndManager] in the Kraft application. */
fun KraftApp.Builder.dnd() = apply {
    setAttribute(DndManager.key, DefaultDndManager())
}

/**
 * A drop zone for payloads of type [T], registered while this component is mounted:
 *
 * ```
 * dropZone<Look>(accepts = { it.payload.kind == slot.kind }) {
 *     onDragStart { canTake = true }
 *     onDragStop { canTake = false; hovered = false }
 *     onEnter { hovered = true }
 *     onLeave { hovered = false }
 *     onDrop { swap(it.payload, it.source) }
 * }
 * ```
 *
 * [element] is the zone's area, by default the component's own DOM element. The handlers keep the component's UI
 * state; the component renders from it (a state change by `value()` redraws by itself). Payloads are matched by class:
 * generic arguments are erased, so a `dropZone<Pair<Look, Int>>` takes any `Pair`; give a payload a class of its own.
 */
inline fun <reified T : Any> Component<*>.dropZone(
    noinline element: () -> Element? = { dom },
    noinline accepts: (Drag<T>) -> Boolean = { true },
    noinline handlers: DropZone.Handlers<T>.() -> Unit,
) = dropZone(type = T::class, element = element, accepts = accepts, handlers = handlers)

/** A drop zone for payloads of [type]; see the reified overload. */
@PublishedApi
internal fun <T : Any> Component<*>.dropZone(
    type: KClass<T>,
    element: () -> Element?,
    accepts: (Drag<T>) -> Boolean,
    handlers: DropZone.Handlers<T>.() -> Unit,
) {
    val zone = DropZone(type = type, element = element, accepts = accepts, handlers = DropZone.Handlers<T>().apply(handlers))
    var unregister: Unsubscribe? = null

    lifecycle {
        onMount { unregister = dnd.register(zone) }

        onUnmount {
            unregister?.invoke()
            unregister = null
        }
    }
}
