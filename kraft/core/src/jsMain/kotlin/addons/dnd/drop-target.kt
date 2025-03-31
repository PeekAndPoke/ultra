package de.peekandpoke.kraft.addons.dnd

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.semanticui.css
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.css.Display
import kotlinx.css.display
import kotlinx.html.Tag
import kotlinx.html.div
import org.w3c.dom.get
import org.w3c.dom.set
import kotlin.reflect.KClass

@Suppress("FunctionName")
inline fun <reified PAYLOAD : Any> Tag.DndDropTarget(
    noinline builder: DndDropTargetBuilder<PAYLOAD>.() -> Unit,
) =
    DndDropTarget(PAYLOAD::class, builder)

@Suppress("FunctionName")
fun <PAYLOAD : Any> Tag.DndDropTarget(
    payloadType: KClass<PAYLOAD>,
    builder: DndDropTargetBuilder<PAYLOAD>.() -> Unit,
) =
    comp(DndDropTargetBuilder(payloadType).apply(builder).build()) { DndDropTargetComponent(it) }

class DndDropTargetBuilder<PAYLOAD : Any>(private val payloadType: KClass<PAYLOAD>) {
    var accepts: (PAYLOAD) -> Boolean = { true }
    var onDrop: (PAYLOAD) -> Unit = {}
    var onMouseOver: (target: Component<*>) -> Unit = {}
    var onMouseOut: (target: Component<*>) -> Unit = {}
    var onDragStart: (target: Component<*>) -> Unit = {}
    var onDragEnd: (target: Component<*>) -> Unit = {}
    var key: String? = null

    internal fun build() = DndDropTargetComponent.Props(
        payloadType = payloadType,
        accepts = accepts,
        onDrop = onDrop,
        onMouseOver = onMouseOver,
        onMouseOut = onMouseOut,
        onDragStart = onDragStart,
        onDragEnd = onDragEnd,
        key = key
    )
}

fun <P : Any> DndDropTargetBuilder<P>.greenHighlights() {
    onMouseOver = {
        it.dom?.apply {
            style.backgroundColor = "rgba(100, 255, 100, 0.3)"
        }
    }
    onMouseOut = {
        it.dom?.apply {
            style.backgroundColor = "rgba(100, 255, 100, 0.05)"
        }
    }
    onDragStart = {
        it.dom?.apply {
            dataset["__dnd_initial__"] = style.backgroundColor
            style.backgroundColor = "rgba(100, 255, 100, 0.05)"
        }
    }
    onDragEnd = {
        it.dom?.apply {
            style.backgroundColor = dataset["__dnd_initial__"] ?: ""
            dataset["__dnd_initial__"] = ""
        }
    }
}

fun <P : Any> DndDropTargetBuilder<P>.blueHighlights() {
    onMouseOver = {
        it.dom?.apply {
            style.backgroundColor = "rgba(100, 100, 255, 0.3)"
        }
    }
    onMouseOut = {
        it.dom?.apply {
            style.backgroundColor = "rgba(100, 100, 255, 0.05)"
        }
    }
    onDragStart = {
        it.dom?.apply {
            dataset["__dnd_initial__"] = style.backgroundColor
            style.backgroundColor = "rgba(100, 100, 255, 0.05)"
        }
    }
    onDragEnd = {
        it.dom?.apply {
            style.backgroundColor = dataset["__dnd_initial__"] ?: ""
            dataset["__dnd_initial__"] = ""
        }
    }
}

class DndDropTargetComponent<PAYLOAD : Any>(ctx: Ctx<Props<PAYLOAD>>) :
    Component<DndDropTargetComponent.Props<PAYLOAD>>(ctx) {

    data class Props<PAYLOAD : Any>(
        val payloadType: KClass<PAYLOAD>,
        val accepts: (PAYLOAD) -> Boolean,
        val onDrop: (PAYLOAD) -> Unit,
        val onMouseOver: (target: Component<*>) -> Unit,
        val onMouseOut: (target: Component<*>) -> Unit,
        val onDragStart: (target: Component<*>) -> Unit,
        val onDragEnd: (target: Component<*>) -> Unit,
        val key: String?,
    )

    init {
        lifecycle.onMount {
            Dnd.registerDropTarget(this)

        }

        lifecycle.onUnmount {
            Dnd.removeDropTarget(this)
        }
    }

    /**
     * Returns true when drop target accepts a payload of the given [payloadClass]
     */
    internal fun acceptsPayloadClass(payloadClass: KClass<*>): Boolean {
        return props.payloadType == payloadClass
    }

    /**
     * Returns true when the drop target accepts the payloads value.
     *
     * This method is called when dragging start, to filter all suitable drop targets.
     */
    internal fun accepts(payload: PAYLOAD) = props.accepts(payload)

    /**
     * Called when something is dropped on the host component.
     */
    internal fun onDrop(payload: PAYLOAD) = props.onDrop(payload)

    /**
     * Called when the mouse is entering the host component
     */
    internal fun onMouseOver() = parent?.let { props.onMouseOver(it) }

    /**
     * Called when the mouse is leaving the host component
     */
    internal fun onMouseOut() = parent?.let { props.onMouseOut(it) }

    /**
     * Called when dragging starts
     */
    internal fun onDragStart() = parent?.let { props.onDragStart(it) }

    /**
     * Called when dragging ends
     */
    internal fun onDragEnd() = parent?.let { props.onDragEnd(it) }

    override fun VDom.render() {
        div {
            css {
                display = Display.none
            }
        }
    }
}
